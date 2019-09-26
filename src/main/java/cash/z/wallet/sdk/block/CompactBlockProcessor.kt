package cash.z.wallet.sdk.block

import androidx.annotation.VisibleForTesting
import cash.z.wallet.sdk.annotation.OpenForTesting
import cash.z.wallet.sdk.data.TransactionRepository
import cash.z.wallet.sdk.data.Twig
import cash.z.wallet.sdk.data.twig
import cash.z.wallet.sdk.exception.CompactBlockProcessorException
import cash.z.wallet.sdk.ext.ZcashSdk.DOWNLOAD_BATCH_SIZE
import cash.z.wallet.sdk.ext.ZcashSdk.MAX_BACKOFF_INTERVAL
import cash.z.wallet.sdk.ext.ZcashSdk.MAX_REORG_SIZE
import cash.z.wallet.sdk.ext.ZcashSdk.POLL_INTERVAL
import cash.z.wallet.sdk.ext.ZcashSdk.RETRIES
import cash.z.wallet.sdk.ext.ZcashSdk.REWIND_DISTANCE
import cash.z.wallet.sdk.ext.ZcashSdk.SAPLING_ACTIVATION_HEIGHT
import cash.z.wallet.sdk.ext.retryUpTo
import cash.z.wallet.sdk.ext.retryWithBackoff
import cash.z.wallet.sdk.jni.RustBackend
import cash.z.wallet.sdk.jni.RustBackendWelding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
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
 */
@OpenForTesting
class CompactBlockProcessor(
    internal val downloader: CompactBlockDownloader,
    private val repository: TransactionRepository,
    private val rustBackend: RustBackendWelding
) {
    var onErrorListener: ((Throwable) -> Boolean)? = null
    var isConnected: Boolean = false
    var isSyncing: Boolean = false
    var isScanning: Boolean = false
    private val progressChannel = ConflatedBroadcastChannel(0)
    private var isStopped = false
    private val consecutiveChainErrors = AtomicInteger(0)

    fun progress(): ReceiveChannel<Int> = progressChannel.openSubscription()

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
                    isSyncing = false
                    isScanning = false
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
        } while (isActive && !isStopped)
        twig("processor complete")
        stop()
    }

    fun stop() {
        isStopped = true
    }

    fun fail(error: Throwable) {
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
        isConnected = true // no exception on downloader call
        isSyncing = true
        val lastDownloadedHeight = max(getLastDownloadedHeight(), SAPLING_ACTIVATION_HEIGHT - 1)
        val lastScannedHeight = getLastScannedHeight()

        twig("latestBlockHeight: $latestBlockHeight\tlastDownloadedHeight: $lastDownloadedHeight" +
                "\tlastScannedHeight: $lastScannedHeight")

        // as long as the database has the sapling tree (like when it's initialized from a checkpoint) we can avoid
        // downloading earlier blocks so take the larger of these two numbers
        val rangeToDownload = (max(lastDownloadedHeight, lastScannedHeight) + 1)..latestBlockHeight
        val rangeToScan = (lastScannedHeight + 1)..latestBlockHeight

        downloadNewBlocks(rangeToDownload)
        val error = validateNewBlocks(rangeToScan)
        if (error < 0) {
            // in theory, a scan should not fail after validation succeeds but maybe consider
            // changing the rust layer to return the failed block height whenever scan does fail
            // rather than a boolean
            val success = scanNewBlocks(rangeToScan)
            if (!success) throw CompactBlockProcessorException.FailedScan
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
                    progressChannel.send(progress)
                    downloadedBlockHeight = end
                }
            }
            Twig.clip("downloading")
        }
        progressChannel.send(100)
    }

    private fun validateNewBlocks(range: IntRange?): Int {
        if (range?.isEmpty() != false) {
            twig("no blocks to validate: $range")
            return -1
        }
        Twig.sprout("validating")
        twig("validating blocks in range $range")
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
        isScanning = true
        val result = rustBackend.scanBlocks()
        isScanning = false
        Twig.clip("scanning")
        return result
    }

    private suspend fun handleChainError(errorHeight: Int) = withContext(IO) {
        val lowerBound = determineLowerBound(errorHeight)
        twig("handling chain error at $errorHeight by rewinding to block $lowerBound")
        rustBackend.rewindToHeight(lowerBound)
        downloader.rewindTo(lowerBound)
    }

    private fun onConnectionError(throwable: Throwable): Boolean {
        isConnected = false
        isSyncing = false
        isScanning = false
        return onErrorListener?.invoke(throwable) ?: true
    }

    private fun determineLowerBound(errorHeight: Int): Int {
        val offset = Math.min(MAX_REORG_SIZE, REWIND_DISTANCE * (consecutiveChainErrors.get() + 1))
        return Math.max(errorHeight - offset, SAPLING_ACTIVATION_HEIGHT)
    }

    suspend fun getLastDownloadedHeight() = withContext(IO) {
        downloader.getLastDownloadedHeight()
    }

    suspend fun getLastScannedHeight() = withContext(IO) {
        repository.lastScannedHeight()
    }
}