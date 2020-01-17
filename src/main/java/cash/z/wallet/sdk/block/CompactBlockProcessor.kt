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
import cash.z.wallet.sdk.ext.ZcashSdk.SCAN_BATCH_SIZE
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
import kotlin.random.Random

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
    private val _processorInfo = ConflatedBroadcastChannel(ProcessorInfo())

    /**
     * The root source of truth for the processor's progress. All processing must be done
     * sequentially, due to the way sqlite works so it is okay for this not to be threadsafe or
     * coroutine safe because processing cannot be concurrent.
     */
    private var currentInfo = ProcessorInfo()

    val state = _state.asFlow()
    val progress = _progress.asFlow()
    val processorInfo = _processorInfo.asFlow()

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
                    delay(calculateDelay())
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
        twig("beginning to process new blocks (with lower bound: $lowerBoundHeight)...")

        // Get the latest info (but don't transmit it on the channel) and then use that to update the scan/download ranges
        ProcessorInfo(
            networkBlockHeight = downloader.getLatestBlockHeight(),
            lastScannedHeight = getLastScannedHeight(),
            lastDownloadedHeight = max(getLastDownloadedHeight(), lowerBoundHeight - 1)
        ).let { initialInfo ->
            updateProgress(
                networkBlockHeight = initialInfo.networkBlockHeight,
                lastScannedHeight = initialInfo.lastScannedHeight,
                lastDownloadedHeight = initialInfo.lastDownloadedHeight,
                lastScanRange = (initialInfo.lastScannedHeight + 1)..initialInfo.networkBlockHeight,
                lastDownloadRange = (max(initialInfo.lastDownloadedHeight, initialInfo.lastScannedHeight) + 1)..initialInfo.networkBlockHeight
            )
        }

        if (currentInfo.lastDownloadRange.isEmpty() && currentInfo.lastScanRange.isEmpty()) {
            twig("Nothing to process: no new blocks to download or scan, right now.")
            setState(Scanned(currentInfo.lastScanRange))
            -1
        } else {
            setState(Downloading)
            downloadNewBlocks(currentInfo.lastDownloadRange)

            setState(Validating)
            var error = validateNewBlocks(currentInfo.lastScanRange)
            if (error < 0) {
                // in theory, a scan should not fail after validation succeeds but maybe consider
                // changing the rust layer to return the failed block height whenever scan does fail
                // rather than a boolean
                setState(Scanning)
                val success = scanNewBlocks(currentInfo.lastScanRange)
                if (!success) throw CompactBlockProcessorException.FailedScan
                else {
                    setState(Scanned(currentInfo.lastScanRange))
                }
                -1
            } else {
                error
            }
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
                    val end = min((range.first + (i * DOWNLOAD_BATCH_SIZE)) - 1, range.last) // subtract 1 on the first value because the range is inclusive
                    var count = 0
                    twig("downloaded $downloadedBlockHeight..$end (batch $i of $batches) [${downloadedBlockHeight..end}]") {
                        count = downloader.downloadBlockRange(downloadedBlockHeight..end)
                    }
                    twig("downloaded $count blocks!")
                    progress = (i / batches.toFloat() * 100).roundToInt()
                    _progress.send(progress)
                    updateProgress(lastDownloadedHeight = downloader.getLastDownloadedHeight().also { twig("updating lastDownloadedHeight=$it") })
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

    private suspend fun scanNewBlocks(range: IntRange?): Boolean = withContext(IO) {
        if (range?.isEmpty() != false) {
            twig("no blocks to scan for range $range")
            true
        } else {
            Twig.sprout("scanning")
            twig("scanning blocks for range $range in batches")
            var result = false
            retryUpTo(3, 500L) {failedAttempts ->
                if (failedAttempts > 0) twig("retrying the scan after $failedAttempts failure(s)...")
                do {
                    var scannedNewBlocks = false
                    result = rustBackend.scanBlocks(SCAN_BATCH_SIZE)
                    val lastScannedHeight = getLastScannedHeight()
                    twig("batch scan complete. Last scanned height: $lastScannedHeight target height: ${range.last}")
                    if (currentInfo.lastScannedHeight != lastScannedHeight) {
                        scannedNewBlocks = true
                        updateProgress(lastScannedHeight = lastScannedHeight)
                    }
                // if we made progress toward our scan, then keep trying
                } while(result && scannedNewBlocks && lastScannedHeight < range.last)
                twig("batch scan complete!")
            }
            Twig.clip("scanning")
            result
        }
    }

    /**
     * @param lastScanRange the inclusive range to scan. This represents what we most recently
     * wanted to scan. In most cases, it will be an invalid range because we'd like to scan blocks
     * that we don't yet have.
     * @param lastDownloadRange the inclusive range to download. This represents what we most
     * recently wanted to scan. In most cases, it will be an invalid range because we'd like to scan
     * blocks that we don't yet have.
     */
    private suspend fun updateProgress(
        networkBlockHeight: Int = currentInfo.networkBlockHeight,
        lastScannedHeight: Int = currentInfo.lastScannedHeight,
        lastDownloadedHeight: Int = currentInfo.lastDownloadedHeight,
        lastScanRange: IntRange = currentInfo.lastScanRange,
        lastDownloadRange: IntRange = currentInfo.lastDownloadRange
    ): Unit = withContext(IO) {
        currentInfo = currentInfo.copy(
            networkBlockHeight = networkBlockHeight,
            lastScannedHeight = lastScannedHeight,
            lastDownloadedHeight = lastDownloadedHeight,
            lastScanRange = lastScanRange,
            lastDownloadRange = lastDownloadRange
        )
        twig("Sending updated currentInfo: $currentInfo")
        _processorInfo.send(currentInfo)
    }

    private suspend fun handleChainError(errorHeight: Int) = withContext(IO) {
        val lowerBound = determineLowerBound(errorHeight)
        twig("handling chain error at $errorHeight by rewinding to block $lowerBound")
        rustBackend.rewindToHeight(lowerBound)
        downloader.rewindToHeight(lowerBound)
    }

    /**
     * Poll on time boundaries and add jitter. Per Issue #95, we want to avoid exposing computation
     * time to a network observer. Instead, we poll at regular time intervals and then also add a
     * little jitter for good measure. See #95 for more details.
     */
    private fun calculateDelay(): Long {
        var nextInterval = System.currentTimeMillis() + POLL_INTERVAL

        // round down to the interval boundary
        nextInterval -= nextInterval.rem(POLL_INTERVAL)

        // add jitter
        return nextInterval + Random.nextLong(POLL_INTERVAL/4)
    }

    private fun onConnectionError(throwable: Throwable): Boolean {
        _state.offer(Disconnected)
        return onErrorListener?.invoke(throwable) ?: true
    }

    private fun determineLowerBound(errorHeight: Int): Int {
        val offset = Math.min(MAX_REORG_SIZE, REWIND_DISTANCE * (consecutiveChainErrors.get() + 1))
        return Math.max(errorHeight - offset, lowerBoundHeight).also {
            twig("offset = min($MAX_REORG_SIZE, $REWIND_DISTANCE * (${consecutiveChainErrors.get() + 1})) = $offset")
            twig("lowerBound = max($errorHeight - $offset, $lowerBoundHeight) = $it")
        }
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
     * @param totalZatoshi the total balance, ignoring funds that cannot be used.
     * @param availableZatoshi the amount of funds that are available for use. Typical reasons that funds
     * may be unavailable include fairly new transactions that do not have enough confirmations or
     * notes that are tied up because we are awaiting change from a transaction. When a note has
     * been spent, its change cannot be used until there are enough confirmations.
     */
    data class WalletBalance(
        val totalZatoshi: Long = -1,
        val availableZatoshi: Long = -1
    )

    /**
     * @param lastDownloadRange inclusive range to download. Meaning, if the range is 10..10,
     * then we will download exactly block 10. If the range is 11..10, then we want to download
     * block 11 but can't.
     * @param lastScanRange inclusive range to scan.
     */
    data class ProcessorInfo(
        val networkBlockHeight: Int = -1,
        val lastScannedHeight: Int = -1,
        val lastDownloadedHeight: Int = -1,
        val lastDownloadRange: IntRange = 0..-1, // empty range
        val lastScanRange: IntRange = 0..-1  // empty range
    ) {
        val hasData get() = networkBlockHeight != -1
                || lastScannedHeight != -1
                || lastDownloadedHeight != -1
                || lastDownloadRange != 0..-1
                || lastScanRange != 0..-1
    }
}