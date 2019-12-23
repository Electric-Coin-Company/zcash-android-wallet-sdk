package cash.z.wallet.sdk.block

import androidx.annotation.VisibleForTesting
import cash.z.wallet.sdk.annotation.OpenForTesting
import cash.z.wallet.sdk.block.CompactBlockProcessor.State.*
import cash.z.wallet.sdk.exception.CompactBlockProcessorException
import cash.z.wallet.sdk.exception.RustLayerException
import cash.z.wallet.sdk.ext.*
import cash.z.wallet.sdk.ext.ZcashSdk.DOWNLOAD_BATCH_SIZE
import cash.z.wallet.sdk.ext.ZcashSdk.MAX_BACKOFF_INTERVAL
import cash.z.wallet.sdk.ext.ZcashSdk.MAX_REORG_SIZE
import cash.z.wallet.sdk.ext.ZcashSdk.POLL_INTERVAL
import cash.z.wallet.sdk.ext.ZcashSdk.RETRIES
import cash.z.wallet.sdk.ext.ZcashSdk.REWIND_DISTANCE
import cash.z.wallet.sdk.ext.ZcashSdk.SAPLING_ACTIVATION_HEIGHT
import cash.z.wallet.sdk.jni.RustBackend
import cash.z.wallet.sdk.jni.RustBackendWelding
import cash.z.wallet.sdk.transaction.TransactionRepository
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Responsible for processing the compact blocks that are received from the lightwallet server. This class encapsulates
 * all the business logic required to validate and scan the blockchain and is therefore tightly coupled with
 * librustzcash.
 *
 * @param minimumHeight the lowest height that we could care about. This is mostly used during
 * reorgs as a backstop to make sure we do not rewind beyond sapling activation. It also is factored
 * in when considering initial range to download. In most cases, this should be the birthday height
 * of the current wallet--the height before which we do not need to scan for transactions.
 */
@OpenForTesting
class CompactBlockProcessor(
    val downloader: CompactBlockDownloader,
    private val repository: TransactionRepository,
    private val rustBackend: RustBackendWelding,
    minimumHeight: Int = SAPLING_ACTIVATION_HEIGHT
) {
    var onErrorListener: ((Throwable) -> Boolean)? = null

    private val consecutiveChainErrors = AtomicInteger(0)
    private val lowerBoundHeight: Int = max(SAPLING_ACTIVATION_HEIGHT, minimumHeight - MAX_REORG_SIZE)

    private val _state: ConflatedBroadcastChannel<State> = ConflatedBroadcastChannel(Initialized)
    private val _progress = ConflatedBroadcastChannel(0)

    val state = _state.asFlow()
    val progress = _progress.asFlow()

    /**
     * Download compact blocks, verify and scan them.
     */
    suspend fun start() = withContext(IO) {
        twig("processor starting")

        // using do/while makes it easier to execute exactly one loop which helps with testing this processor quickly
        // (because you can start and then immediately set isStopped=true to always get precisely one loop)
        do {
            retryWithBackoff(::onConnectionError, maxDelayMillis = MAX_BACKOFF_INTERVAL) {
                val result = processNewBlocks()
                // immediately process again after failures in order to download new blocks right away
                if (result < 0) {
                    consecutiveChainErrors.set(0)
                    twig("Successfully processed new blocks. Sleeping for ${POLL_INTERVAL}ms")
                    delay(POLL_INTERVAL)
                } else {
                    if(consecutiveChainErrors.get() >= RETRIES) {
                        val errorMessage = "ERROR: unable to resolve reorg at height $result after ${consecutiveChainErrors.get()} correction attempts!"
                        fail(CompactBlockProcessorException.FailedReorgRepair(errorMessage))
                    } else {
                        handleChainError(result)
                    }
                    consecutiveChainErrors.getAndIncrement()
                }
            }
        } while (isActive && !_state.isClosedForSend && _state.value !is Stopped)
        twig("processor complete")
        stop()
    }

    suspend fun stop() {
        setState(Stopped)
    }

    private suspend fun fail(error: Throwable) {
        stop()
        twig("${error.message}")
        throw error
    }

    /**
     * Process new blocks returning false whenever an error was found.
     *
     * @return -1 when processing was successful and did not encounter errors during validation or scanning. Otherwise
     * return the block height where an error was found.
     */
    private suspend fun processNewBlocks(): Int = withContext(IO) {
        twig("beginning to process new blocks...")

        // define ranges
        val latestBlockHeight = downloader.getLatestBlockHeight()
        val lastDownloadedHeight = getLastDownloadedHeight()
        val lastScannedHeight = getLastScannedHeight()
        val boundedLastDownloadedHeight = max(lastDownloadedHeight, lowerBoundHeight - 1)

        twig(
            "latestBlockHeight: $latestBlockHeight\tlastDownloadedHeight: $lastDownloadedHeight" +
                    "\tlastScannedHeight: $lastScannedHeight\tlowerBoundHeight: $lowerBoundHeight"
        )

        // as long as the database has the sapling tree (like when it's initialized from a checkpoint) we can avoid
        // downloading earlier blocks so take the larger of these two numbers
        val rangeToDownload = (max(boundedLastDownloadedHeight, lastScannedHeight) + 1)..latestBlockHeight
        val rangeToScan = (lastScannedHeight + 1)..latestBlockHeight

        setState(Downloading)
        downloadNewBlocks(rangeToDownload)

        setState(Validating)
        var error = validateNewBlocks(rangeToScan)
        if (error < 0) {
            // in theory, a scan should not fail after validation succeeds but maybe consider
            // changing the rust layer to return the failed block height whenever scan does fail
            // rather than a boolean
            setState(Scanning)
            val success = scanNewBlocks(rangeToScan)
            if (!success) throw CompactBlockProcessorException.FailedScan
            else {
                setState(Scanned(rangeToScan))
            }
            -1
        } else {
            error
        }
    }



    @VisibleForTesting //allow mocks to verify how this is called, rather than the downloader, which is more complex
    internal suspend fun downloadNewBlocks(range: IntRange) = withContext<Unit>(IO) {
        if (range.isEmpty()) {
            twig("no blocks to download")
        } else {
            _state.send(Downloading)
            Twig.sprout("downloading")
            twig("downloading blocks in range $range")

            var downloadedBlockHeight = range.first
            val missingBlockCount = range.last - range.first + 1
            val batches = (missingBlockCount / DOWNLOAD_BATCH_SIZE
                    + (if (missingBlockCount.rem(DOWNLOAD_BATCH_SIZE) == 0) 0 else 1))
            var progress: Int
            twig("found $missingBlockCount missing blocks, downloading in $batches batches of ${DOWNLOAD_BATCH_SIZE}...")
            for (i in 1..batches) {
                retryUpTo(RETRIES) {
                    val end = min(range.first + (i * DOWNLOAD_BATCH_SIZE), range.last + 1)
                    twig("downloaded $downloadedBlockHeight..${(end - 1)} (batch $i of $batches)") {
                        downloader.downloadBlockRange(downloadedBlockHeight until end)
                    }
                    progress = (i / batches.toFloat() * 100).roundToInt()
                    // only report during large downloads. TODO: allow for configuration of "large"
                    _progress.send(progress)
                    downloadedBlockHeight = end
                }
            }
            Twig.clip("downloading")
        }
        _progress.send(100)
    }

    private fun validateNewBlocks(range: IntRange?): Int {
        if (range?.isEmpty() != false) {
            twig("no blocks to validate: $range")
            return -1
        }
        Twig.sprout("validating")
        twig("validating blocks in range $range in db: ${(rustBackend as RustBackend).dbCachePath}")
        val result = rustBackend.validateCombinedChain()
        Twig.clip("validating")
        return result
    }

    private fun scanNewBlocks(range: IntRange?): Boolean {
        if (range?.isEmpty() != false) {
            twig("no blocks to scan")
            return true
        }
        Twig.sprout("scanning")
        twig("scanning blocks in range $range")
        val result = rustBackend.scanBlocks()
        Twig.clip("scanning")
        return result
    }

    private suspend fun handleChainError(errorHeight: Int) = withContext(IO) {
        val lowerBound = determineLowerBound(errorHeight)
        twig("handling chain error at $errorHeight by rewinding to block $lowerBound")
        rustBackend.rewindToHeight(lowerBound)
        downloader.rewindToHeight(lowerBound)
    }

    private fun onConnectionError(throwable: Throwable): Boolean {
        _state.offer(Disconnected)
        return onErrorListener?.invoke(throwable) ?: true
    }

    private fun determineLowerBound(errorHeight: Int): Int {
        val offset = Math.min(MAX_REORG_SIZE, REWIND_DISTANCE * (consecutiveChainErrors.get() + 1))
        return Math.max(errorHeight - offset, lowerBoundHeight)
    }

    suspend fun getLastDownloadedHeight() = withContext(IO) {
        downloader.getLastDownloadedHeight()
    }

    suspend fun getLastScannedHeight() = withContext(IO) {
        repository.lastScannedHeight()
    }

    suspend fun getAddress(accountId: Int) = withContext(IO) {
        rustBackend.getAddress(accountId)
    }

    /**
     * Calculates the latest balance info. Defaults to the first account.
     *
     * @param accountIndex the account to check for balance info.
     */
    suspend fun getBalanceInfo(accountIndex: Int = 0): WalletBalance = withContext(IO) {
        twigTask("checking balance info") {
            try {
                val balanceTotal = rustBackend.getBalance(accountIndex)
                twig("found total balance of: $balanceTotal")
                val balanceAvailable = rustBackend.getVerifiedBalance(accountIndex)
                twig("found available balance of: $balanceAvailable")
                WalletBalance(balanceTotal, balanceAvailable)
            } catch (t: Throwable) {
                twig("failed to get balance due to $t")
                throw RustLayerException.BalanceException(t)
            }
        }
    }

    suspend fun setState(newState: State) {
        _state.send(newState)
    }

    sealed class State {
        interface Connected
        interface Syncing
        object Downloading : Connected, Syncing, State()
        object Validating : Connected, Syncing, State()
        object Scanning : Connected, Syncing, State()
        class Scanned(val scannedRange:IntRange) : Connected, Syncing, State()
        object Disconnected : State()
        object Stopped : State()
        object Initialized : State()
    }

    /**
     * Data structure to hold the total and available balance of the wallet. This is what is
     * received on the balance channel.
     *
     * @param total the total balance, ignoring funds that cannot be used.
     * @param available the amount of funds that are available for use. Typical reasons that funds
     * may be unavailable include fairly new transactions that do not have enough confirmations or
     * notes that are tied up because we are awaiting change from a transaction. When a note has
     * been spent, its change cannot be used until there are enough confirmations.
     */
    data class WalletBalance(
        val total: Long = -1,
        val available: Long = -1
    )
}