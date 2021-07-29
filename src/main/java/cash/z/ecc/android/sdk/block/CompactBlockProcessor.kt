package cash.z.ecc.android.sdk.block

import androidx.annotation.VisibleForTesting
import cash.z.ecc.android.sdk.BuildConfig
import cash.z.ecc.android.sdk.annotation.OpenForTesting
import cash.z.ecc.android.sdk.block.CompactBlockProcessor.State.Disconnected
import cash.z.ecc.android.sdk.block.CompactBlockProcessor.State.Downloading
import cash.z.ecc.android.sdk.block.CompactBlockProcessor.State.Enhancing
import cash.z.ecc.android.sdk.block.CompactBlockProcessor.State.Initialized
import cash.z.ecc.android.sdk.block.CompactBlockProcessor.State.Scanned
import cash.z.ecc.android.sdk.block.CompactBlockProcessor.State.Scanning
import cash.z.ecc.android.sdk.block.CompactBlockProcessor.State.Stopped
import cash.z.ecc.android.sdk.block.CompactBlockProcessor.State.Validating
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException.EnhanceTransactionError.EnhanceTxDecryptError
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException.EnhanceTransactionError.EnhanceTxDownloadError
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException.MismatchedBranch
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException.MismatchedNetwork
import cash.z.ecc.android.sdk.exception.InitializerException
import cash.z.ecc.android.sdk.exception.RustLayerException
import cash.z.ecc.android.sdk.ext.BatchMetrics
import cash.z.ecc.android.sdk.ext.Twig
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.ZcashSdk.DOWNLOAD_BATCH_SIZE
import cash.z.ecc.android.sdk.ext.ZcashSdk.MAX_BACKOFF_INTERVAL
import cash.z.ecc.android.sdk.ext.ZcashSdk.MAX_REORG_SIZE
import cash.z.ecc.android.sdk.ext.ZcashSdk.POLL_INTERVAL
import cash.z.ecc.android.sdk.ext.ZcashSdk.RETRIES
import cash.z.ecc.android.sdk.ext.ZcashSdk.REWIND_DISTANCE
import cash.z.ecc.android.sdk.ext.ZcashSdk.SCAN_BATCH_SIZE
import cash.z.ecc.android.sdk.ext.retryUpTo
import cash.z.ecc.android.sdk.ext.retryWithBackoff
import cash.z.ecc.android.sdk.ext.toHexReversed
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.ext.twigTask
import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.jni.RustBackendWelding
import cash.z.ecc.android.sdk.transaction.PagedTransactionRepository
import cash.z.ecc.android.sdk.transaction.TransactionRepository
import cash.z.ecc.android.sdk.type.WalletBalance
import cash.z.wallet.sdk.rpc.Service
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Responsible for processing the compact blocks that are received from the lightwallet server. This class encapsulates
 * all the business logic required to validate and scan the blockchain and is therefore tightly coupled with
 * librustzcash.
 *
 * @property downloader the component responsible for downloading compact blocks and persisting them
 * locally for processing.
 * @property repository the repository holding transaction information.
 * @property rustBackend the librustzcash functionality available and exposed to the SDK.
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
    minimumHeight: Int = rustBackend.network.saplingActivationHeight
) {
    /**
     * Callback for any non-trivial errors that occur while processing compact blocks.
     *
     * @return true when processing should continue. Return false when the error is unrecoverable
     * and all processing should halt and stop retrying.
     */
    var onProcessorErrorListener: ((Throwable) -> Boolean)? = null

    /**
     * Callback for reorgs. This callback is invoked when validation fails with the height at which
     * an error was found and the lower bound to which the data will rewind, at most.
     */
    var onChainErrorListener: ((errorHeight: Int, rewindHeight: Int) -> Any)? = null

    /**
     * Callback for setup errors that occur prior to processing compact blocks. Can be used to
     * override any errors from [verifySetup]. When this listener is missing then all setup errors
     * will result in the processor not starting. This is particularly useful for wallets to receive
     * a callback right before the SDK will reject a lightwalletd server because it appears not to
     * match.
     *
     * @return true when the setup error should be ignored and processing should be allowed to
     * start. Otherwise, processing will not begin.
     */
    var onSetupErrorListener: ((Throwable) -> Boolean)? = null

    /**
     * Callback for apps to report scan times. As blocks are scanned in batches, this listener is
     * invoked at the end of every batch and the second parameter is only true when all batches are
     * complete. The first parameter contains useful information on the blocks scanned per second.
     *
     * The Boolean param (isComplete) is true when this event represents the completion of a scan
     */
    var onScanMetricCompleteListener: ((BatchMetrics, Boolean) -> Unit)? = null

    private val consecutiveChainErrors = AtomicInteger(0)
    private val lowerBoundHeight: Int = max(rustBackend.network.saplingActivationHeight, minimumHeight - MAX_REORG_SIZE)

    private val _state: ConflatedBroadcastChannel<State> = ConflatedBroadcastChannel(Initialized)
    private val _progress = ConflatedBroadcastChannel(0)
    private val _processorInfo = ConflatedBroadcastChannel(ProcessorInfo())
    private val _networkHeight = MutableStateFlow(-1)
    private val processingMutex = Mutex()

    /**
     * Flow of birthday heights. The birthday is essentially the first block that the wallet cares
     * about. Any prior block can be ignored. This is not a fixed value because the height is
     * influenced by the first transaction, which isn't always known. So we start with an estimation
     * and improve it as the wallet progresses. Once the first transaction occurs, this value is
     * effectively fixed.
     */
    private val _birthdayHeight = MutableStateFlow(lowerBoundHeight)

    /**
     * The root source of truth for the processor's progress. All processing must be done
     * sequentially, due to the way sqlite works so it is okay for this not to be threadsafe or
     * coroutine safe because processing cannot be concurrent.
     */
    internal var currentInfo = ProcessorInfo()

    /**
     * The zcash network that is being processed. Either Testnet or Mainnet.
     */
    val network = rustBackend.network

    /**
     * The flow of state values so that a wallet can monitor the state of this class without needing
     * to poll.
     */
    val state = _state.asFlow()

    /**
     * The flow of progress values so that a wallet can monitor how much downloading remains
     * without needing to poll.
     */
    val progress = _progress.asFlow()

    /**
     * The flow of detailed processorInfo like the range of blocks that shall be downloaded and
     * scanned. This gives the wallet a lot of insight into the work of this processor.
     */
    val processorInfo = _processorInfo.asFlow()

    /**
     * The flow of network height. This value is updated at the same time that [currentInfo] is
     * updated but this allows consumers to have the information pushed instead of polling.
     */
    val networkHeight = _networkHeight.asStateFlow()

    /**
     * The first block this wallet cares about anything prior can be ignored. If a wallet has no
     * transactions, this value will later update to 100 blocks before the first transaction,
     * rounded down to the nearest 100. So in some cases, this is a dynamic value.
     */
    val birthdayHeight = _birthdayHeight.value

    /**
     * Download compact blocks, verify and scan them until [stop] is called.
     */
    suspend fun start() = withContext(IO) {
        verifySetup()
        updateBirthdayHeight()
        twig("setup verified. processor starting")

        // using do/while makes it easier to execute exactly one loop which helps with testing this processor quickly
        // (because you can start and then immediately set isStopped=true to always get precisely one loop)
        do {
            retryWithBackoff(::onProcessorError, maxDelayMillis = MAX_BACKOFF_INTERVAL) {
                val result = processingMutex.withLockLogged("processNewBlocks") {
                    processNewBlocks()
                }
                // immediately process again after failures in order to download new blocks right away
                if (result == ERROR_CODE_RECONNECT) {
                    val napTime = calculatePollInterval(true)
                    twig("Unable to process new blocks because we are disconnected! Attempting to reconnect in ${napTime}ms")
                    delay(napTime)
                } else if (result == ERROR_CODE_NONE || result == ERROR_CODE_FAILED_ENHANCE) {
                    val noWorkDone = currentInfo.lastDownloadRange.isEmpty() && currentInfo.lastScanRange.isEmpty()
                    val summary = if (noWorkDone) "Nothing to process: no new blocks to download or scan" else "Done processing blocks"
                    consecutiveChainErrors.set(0)
                    val napTime = calculatePollInterval()
                    twig("$summary${if (result == ERROR_CODE_FAILED_ENHANCE) " (but there were enhancement errors! We ignore those, for now. Memos in this block range are probably missing! This will be improved in a future release.)" else ""}! Sleeping for ${napTime}ms (latest height: ${currentInfo.networkBlockHeight}).")
                    delay(napTime)
                } else {
                    if (consecutiveChainErrors.get() >= RETRIES) {
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

    /**
     * Sets the state to [Stopped], which causes the processor loop to exit.
     */
    suspend fun stop() {
        runCatching {
            setState(Stopped)
            downloader.stop()
        }
    }

    /**
     * Stop processing and throw an error.
     */
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
        twig("beginning to process new blocks (with lower bound: $lowerBoundHeight)...", -1)

        if (!updateRanges()) {
            twig("Disconnection detected! Attempting to reconnect!")
            setState(Disconnected)
            downloader.lightWalletService.reconnect()
            ERROR_CODE_RECONNECT
        } else if (currentInfo.lastDownloadRange.isEmpty() && currentInfo.lastScanRange.isEmpty()) {
            setState(Scanned(currentInfo.lastScanRange))
            ERROR_CODE_NONE
        } else {
            downloadNewBlocks(currentInfo.lastDownloadRange)
            val error = validateAndScanNewBlocks(currentInfo.lastScanRange)
            if (error != ERROR_CODE_NONE) error else {
                enhanceTransactionDetails(currentInfo.lastScanRange)
            }
        }
    }

    /**
     * Gets the latest range info and then uses that initialInfo to update (and transmit)
     * the scan/download ranges that require processing.
     *
     * @return true when the update succeeds.
     */
    private suspend fun updateRanges(): Boolean = withContext(IO) {
        try {
            // TODO: rethink this and make it easier to understand what's happening. Can we reduce this
            // so that we only work with actual changing info rather than periodic snapshots? Do we need
            // to calculate these derived values every time?
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
                    lastDownloadRange = (
                        max(
                            initialInfo.lastDownloadedHeight,
                            initialInfo.lastScannedHeight
                        ) + 1
                        )..initialInfo.networkBlockHeight
                )
            }
            true
        } catch (t: StatusRuntimeException) {
            twig("Warning: failed to update ranges due to $t caused by ${t.cause}")
            false
        }
    }

    /**
     * Given a range, validate and then scan all blocks. Validation is ensuring that the blocks are
     * in ascending order, with no gaps and are also chain-sequential. This means every block's
     * prevHash value matches the preceding block in the chain.
     *
     * @param lastScanRange the range to be validated and scanned.
     *
     * @return error code or [ERROR_CODE_NONE] when there is no error.
     */
    private suspend fun validateAndScanNewBlocks(lastScanRange: IntRange): Int = withContext(IO) {
        setState(Validating)
        var error = validateNewBlocks(lastScanRange)
        if (error == ERROR_CODE_NONE) {
            // in theory, a scan should not fail after validation succeeds but maybe consider
            // changing the rust layer to return the failed block height whenever scan does fail
            // rather than a boolean
            setState(Scanning)
            val success = scanNewBlocks(lastScanRange)
            if (!success) throw CompactBlockProcessorException.FailedScan()
            else {
                setState(Scanned(lastScanRange))
            }
            ERROR_CODE_NONE
        } else {
            error
        }
    }

    private suspend fun enhanceTransactionDetails(lastScanRange: IntRange): Int {
        Twig.sprout("enhancing")
        twig("Enhancing transaction details for blocks $lastScanRange")
        setState(Enhancing)
        return try {
            val newTxs = repository.findNewTransactions(lastScanRange)
            if (newTxs == null) {
                twig("no new transactions found in $lastScanRange")
            } else {
                twig("enhancing ${newTxs.size} transaction(s)!")
                // if the first transaction has been added
                if (newTxs.size == repository.count()) {
                    twig("Encountered the first transaction. This changes the birthday height!")
                    updateBirthdayHeight()
                }
            }

            newTxs?.onEach { newTransaction ->
                if (newTransaction == null) twig("somehow, new transaction was null!!!")
                else enhance(newTransaction)
            }
            twig("Done enhancing transaction details")
            ERROR_CODE_NONE
        } catch (t: Throwable) {
            twig("Failed to enhance due to $t")
            t.printStackTrace()
            ERROR_CODE_FAILED_ENHANCE
        } finally {
            Twig.clip("enhancing")
        }
    }

    // TODO: we still need a way to identify those transactions that failed to be enhanced
    private suspend fun enhance(transaction: ConfirmedTransaction) = withContext(Dispatchers.IO) {
        var downloaded = false
        try {
            twig("START: enhancing transaction (id:${transaction.id}  block:${transaction.minedHeight})")
            downloader.fetchTransaction(transaction.rawTransactionId)?.let { tx ->
                downloaded = true
                twig("decrypting and storing transaction (id:${transaction.id}  block:${transaction.minedHeight})")
                rustBackend.decryptAndStoreTransaction(tx.data.toByteArray())
            } ?: twig("no transaction found. Nothing to enhance. This probably shouldn't happen.")
            twig("DONE: enhancing transaction (id:${transaction.id}  block:${transaction.minedHeight})")
        } catch (t: Throwable) {
            twig("Warning: failure on transaction: error: $t\ttransaction: $transaction")
            onProcessorError(
                if (downloaded) EnhanceTxDecryptError(transaction.minedHeight, t)
                else EnhanceTxDownloadError(transaction.minedHeight, t)
            )
        }
    }

    /**
     * Confirm that the wallet data is properly setup for use.
     */
    private suspend fun verifySetup() {
        // verify that the data is initialized
        var error = when {
            !repository.isInitialized() -> CompactBlockProcessorException.Uninitialized
            repository.getAccountCount() == 0 -> CompactBlockProcessorException.NoAccount
            else -> {
                // verify that the server is correct
                downloader.getServerInfo().let { info ->
                    val clientBranch = "%x".format(rustBackend.getBranchIdForHeight(info.blockHeight.toInt()))
                    val network = rustBackend.network.networkName
                    when {
                        !info.matchingNetwork(network) -> MismatchedNetwork(clientNetwork = network, serverNetwork = info.chainName)
                        !info.matchingConsensusBranchId(clientBranch) -> MismatchedBranch(clientBranch = clientBranch, serverBranch = info.consensusBranchId, networkName = network)
                        else -> null
                    }
                }
            }
        }

        if (error != null) {
            twig("Validating setup prior to scanning . . . ISSUE FOUND! - ${error.javaClass.simpleName}")
            // give listener a chance to override
            if (onSetupErrorListener?.invoke(error) != true) {
                throw error
            } else {
                twig("Warning: An ${error::class.java.simpleName} was encountered while verifying setup but it was ignored by the onSetupErrorHandler. Ignoring message: ${error.message}")
            }
        }
    }

    private suspend fun updateBirthdayHeight() {
        try {
            val betterBirthday = calculateBirthdayHeight()
            if (betterBirthday > birthdayHeight) {
                twig("Better birthday found! Birthday height updated from $birthdayHeight to $betterBirthday")
                _birthdayHeight.value = betterBirthday
            }
        } catch (e: Throwable) {
            twig("Warning: updating the birthday height failed due to $e")
        }
    }

    var failedUtxoFetches = 0
    internal suspend fun refreshUtxos(tAddress: String, startHeight: Int): Int? = withContext(IO) {
        var count: Int? = null
        // todo: cleanup the way that we prevent this from running excessively
        //       For now, try for about 3 blocks per app launch. If the service fails it is
        //       probably disabled on ligthtwalletd, so then stop trying until the next app launch.
        if (failedUtxoFetches < 9) { // there are 3 attempts per block
            try {
                retryUpTo(3) {
                    val result = downloader.lightWalletService.fetchUtxos(tAddress, startHeight)
                    count = processUtxoResult(result, tAddress, startHeight)
                }
            } catch (e: Throwable) {
                failedUtxoFetches++
                twig("Warning: Fetching UTXOs is repeatedly failing! We will only try about ${(9 - failedUtxoFetches + 2) / 3} more times then give up for this session.")
            }
        } else {
            twig("Warning: gave up on fetching UTXOs for this session. It seems to unavailable on lightwalletd.")
        }
        count
    }

    internal suspend fun processUtxoResult(result: List<Service.GetAddressUtxosReply>, tAddress: String, startHeight: Int): Int = withContext(IO) {
        var skipped = 0
        val aboveHeight = startHeight - 1
        twig("Clearing utxos above height $aboveHeight", -1)
        rustBackend.clearUtxos(tAddress, aboveHeight)
        twig("Checking for UTXOs above height $aboveHeight")
        result.forEach { utxo: Service.GetAddressUtxosReply ->
            twig("Found UTXO at height ${utxo.height.toInt()} with ${utxo.valueZat} zatoshi")
            try {
                rustBackend.putUtxo(
                    tAddress,
                    utxo.txid.toByteArray(),
                    utxo.index,
                    utxo.script.toByteArray(),
                    utxo.valueZat,
                    utxo.height.toInt()
                )
            } catch (t: Throwable) {
                // TODO: more accurately track the utxos that were skipped (in theory, this could fail for other reasons)
                skipped++
                twig("Warning: Ignoring transaction at height ${utxo.height} @ index ${utxo.index} because it already exists")
            }
        }
        // return the number of UTXOs that were downloaded
        result.size - skipped
    }

    /**
     * Request all blocks in the given range and persist them locally for processing, later.
     *
     * @param range the range of blocks to download.
     */
    @VisibleForTesting // allow mocks to verify how this is called, rather than the downloader, which is more complex
    internal suspend fun downloadNewBlocks(range: IntRange) = withContext<Unit>(IO) {
        if (range.isEmpty()) {
            twig("no blocks to download")
        } else {
            _state.send(Downloading)
            Twig.sprout("downloading")
            twig("downloading blocks in range $range", -1)

            var downloadedBlockHeight = range.first
            val missingBlockCount = range.last - range.first + 1
            val batches = (
                missingBlockCount / DOWNLOAD_BATCH_SIZE +
                    (if (missingBlockCount.rem(DOWNLOAD_BATCH_SIZE) == 0) 0 else 1)
                )
            var progress: Int
            twig("found $missingBlockCount missing blocks, downloading in $batches batches of $DOWNLOAD_BATCH_SIZE...")
            for (i in 1..batches) {
                retryUpTo(RETRIES, { CompactBlockProcessorException.FailedDownload(it) }) {
                    val end = min((range.first + (i * DOWNLOAD_BATCH_SIZE)) - 1, range.last) // subtract 1 on the first value because the range is inclusive
                    var count = 0
                    twig("downloaded $downloadedBlockHeight..$end (batch $i of $batches) [${downloadedBlockHeight..end}]") {
                        count = downloader.downloadBlockRange(downloadedBlockHeight..end)
                    }
                    twig("downloaded $count blocks!")
                    progress = (i / batches.toFloat() * 100).roundToInt()
                    _progress.send(progress)
                    val lastDownloadedHeight = downloader.getLastDownloadedHeight().takeUnless { it < network.saplingActivationHeight } ?: -1
                    updateProgress(lastDownloadedHeight = lastDownloadedHeight)
                    downloadedBlockHeight = end
                }
            }
            Twig.clip("downloading")
        }
        _progress.send(100)
    }

    /**
     * Validate all blocks in the given range, ensuring that the blocks are in ascending order, with
     * no gaps and are also chain-sequential. This means every block's prevHash value matches the
     * preceding block in the chain.
     *
     *  @param range the range of blocks to validate.
     *
     *  @return [ERROR_CODE_NONE] when there is no problem. Otherwise, return the lowest height where an error was
     *  found. In other words, validation starts at the back of the chain and works toward the tip.
     */
    private fun validateNewBlocks(range: IntRange?): Int {
        if (range?.isEmpty() != false) {
            twig("no blocks to validate: $range")
            return ERROR_CODE_NONE
        }
        Twig.sprout("validating")
        twig("validating blocks in range $range in db: ${(rustBackend as RustBackend).pathCacheDb}")
        val result = rustBackend.validateCombinedChain()
        Twig.clip("validating")
        return result
    }

    /**
     * Scan all blocks in the given range, decrypting and persisting anything that matches our
     * wallet.
     *
     *  @param range the range of blocks to scan.
     *
     *  @return [ERROR_CODE_NONE] when there is no problem. Otherwise, return the lowest height where an error was
     *  found. In other words, scanning starts at the back of the chain and works toward the tip.
     */
    private suspend fun scanNewBlocks(range: IntRange?): Boolean = withContext(IO) {
        if (range?.isEmpty() != false) {
            twig("no blocks to scan for range $range")
            true
        } else {
            Twig.sprout("scanning")
            twig("scanning blocks for range $range in batches")
            var result = false
            var metrics = BatchMetrics(range, SCAN_BATCH_SIZE, onScanMetricCompleteListener)
            // Attempt to scan a few times to work around any concurrent modification errors, then
            // rethrow as an official processorError which is handled by [start.retryWithBackoff]
            retryUpTo(3, { CompactBlockProcessorException.FailedScan(it) }) { failedAttempts ->
                if (failedAttempts > 0) twig("retrying the scan after $failedAttempts failure(s)...")
                do {
                    var scannedNewBlocks = false
                    metrics.beginBatch()
                    result = rustBackend.scanBlocks(SCAN_BATCH_SIZE)
                    metrics.endBatch()
                    val lastScannedHeight = range.start + metrics.cumulativeItems - 1
                    val percentValue = (lastScannedHeight - range.first) / (range.last - range.first + 1).toFloat() * 100.0f
                    val percent = "%.0f".format(percentValue.coerceAtMost(100f).coerceAtLeast(0f))
                    twig("batch scanned ($percent%): $lastScannedHeight/${range.last} | ${metrics.batchTime}ms, ${metrics.batchItems}blks, ${metrics.batchIps.format()}bps")
                    if (currentInfo.lastScannedHeight != lastScannedHeight) {
                        scannedNewBlocks = true
                        updateProgress(lastScannedHeight = lastScannedHeight)
                    }
                    // if we made progress toward our scan, then keep trying
                } while (result && scannedNewBlocks && lastScannedHeight < range.last)
                twig("batch scan complete! Total time: ${metrics.cumulativeTime}  Total blocks measured: ${metrics.cumulativeItems}  Cumulative bps: ${metrics.cumulativeIps.format()}")
            }
            Twig.clip("scanning")
            result
        }
    }

    private fun Float.format(places: Int = 0) = "%.${places}f".format(this)

    /**
     * Emit an instance of processorInfo, corresponding to the provided data.
     *
     * @param networkBlockHeight the latest block available to lightwalletd that may or may not be
     * downloaded by this wallet yet.
     * @param lastScannedHeight the height up to which the wallet last scanned. This determines
     * where the next scan will begin.
     * @param lastDownloadedHeight the last compact block that was successfully downloaded.
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
        _networkHeight.value = networkBlockHeight
        _processorInfo.send(currentInfo)
    }

    private suspend fun handleChainError(errorHeight: Int) {
        // TODO consider an error object containing hash information
        printValidationErrorInfo(errorHeight)
        determineLowerBound(errorHeight).let { lowerBound ->
            twig("handling chain error at $errorHeight by rewinding to block $lowerBound")
            onChainErrorListener?.invoke(errorHeight, lowerBound)
            rewindToNearestHeight(lowerBound, true)
        }
    }

    suspend fun getNearestRewindHeight(height: Int): Int {
        // TODO: add a concept of original checkpoint height to the processor. For now, derive it
        val originalCheckpoint = lowerBoundHeight + MAX_REORG_SIZE + 2 // add one because we already have the checkpoint. Add one again because we delete ABOVE the block
        return if (height < originalCheckpoint) {
            originalCheckpoint
        } else {
            // tricky: subtract one because we delete ABOVE this block
            rustBackend.getNearestRewindHeight(height) - 1
        }
    }

    /**
     * Rewind back at least two weeks worth of blocks.
     */
    suspend fun quickRewind() = withContext(IO) {
        val height = max(currentInfo.lastScannedHeight, repository.lastScannedHeight())
        val blocksPerDay = 60 * 60 * 24 * 1000 / ZcashSdk.BLOCK_INTERVAL_MILLIS.toInt()
        val twoWeeksBack = (height - blocksPerDay * 14).coerceAtLeast(lowerBoundHeight)
        rewindToNearestHeight(twoWeeksBack, false)
    }

    /**
     * @param alsoClearBlockCache when true, also clear the block cache which forces a redownload of
     * blocks. Otherwise, the cached blocks will be used in the rescan, which in most cases, is fine.
     */
    suspend fun rewindToNearestHeight(height: Int, alsoClearBlockCache: Boolean = false) = withContext(IO) {
        processingMutex.withLockLogged("rewindToHeight") {
            val lastScannedHeight = currentInfo.lastScannedHeight
            val lastLocalBlock = repository.lastScannedHeight()
            val targetHeight = getNearestRewindHeight(height)
            twig("Rewinding from $lastScannedHeight to requested height: $height using target height: $targetHeight with last local block: $lastLocalBlock")
            if (targetHeight < lastScannedHeight || (lastScannedHeight == -1 && (targetHeight < lastLocalBlock))) {
                rustBackend.rewindToHeight(targetHeight)
            } else {
                twig("not rewinding dataDb because the last scanned height is $lastScannedHeight and the last local block is $lastLocalBlock both of which are less than the target height of $targetHeight")
            }

            if (alsoClearBlockCache) {
                twig("Also clearing block cache back to $targetHeight. These rewound blocks will download in the next scheduled scan")
                downloader.rewindToHeight(targetHeight)
                // communicate that the wallet is no longer synced because it might remain this way for 20+ seconds because we only download on 20s time boundaries so we can't trigger any immediate action
                setState(Downloading)
                updateProgress(
                    lastScannedHeight = targetHeight,
                    lastDownloadedHeight = targetHeight,
                    lastScanRange = (targetHeight + 1)..currentInfo.networkBlockHeight,
                    lastDownloadRange = (targetHeight + 1)..currentInfo.networkBlockHeight
                )
                _progress.send(0)
            } else {
                updateProgress(
                    lastScannedHeight = targetHeight,
                    lastScanRange = (targetHeight + 1)..currentInfo.networkBlockHeight,
                )
                _progress.send(0)
                val range = (targetHeight + 1)..lastScannedHeight
                twig("We kept the cache blocks in place so we don't need to wait for the next scheduled download to rescan. Instead we will rescan and validate blocks ${range.first}..${range.last}")
                if (validateAndScanNewBlocks(range) == ERROR_CODE_NONE) enhanceTransactionDetails(range)
            }
        }
    }

    /** insightful function for debugging these critical errors */
    private suspend fun printValidationErrorInfo(errorHeight: Int, count: Int = 11) {
        // Note: blocks are public information so it's okay to print them but, still, let's not unless we're debugging something
        if (!BuildConfig.DEBUG) return

        var errorInfo = fetchValidationErrorInfo(errorHeight)
        twig("validation failed at block ${errorInfo.errorHeight} which had hash ${errorInfo.actualPrevHash} but the expected hash was ${errorInfo.expectedPrevHash}")
        errorInfo = fetchValidationErrorInfo(errorHeight + 1)
        twig("The next block block: ${errorInfo.errorHeight} which had hash ${errorInfo.actualPrevHash} but the expected hash was ${errorInfo.expectedPrevHash}")

        twig("=================== BLOCKS [$errorHeight..${errorHeight + count - 1}]: START ========")
        repeat(count) { i ->
            val height = errorHeight + i
            val block = downloader.compactBlockStore.findCompactBlock(height)
            // sometimes the initial block was inserted via checkpoint and will not appear in the cache. We can get the hash another way but prevHash is correctly null.
            val hash = block?.hash?.toByteArray() ?: (repository as PagedTransactionRepository).findBlockHash(height)
            twig("block: $height\thash=${hash?.toHexReversed()} \tprevHash=${block?.prevHash?.toByteArray()?.toHexReversed()}")
        }
        twig("=================== BLOCKS [$errorHeight..${errorHeight + count - 1}]: END ========")
    }

    private suspend fun fetchValidationErrorInfo(errorHeight: Int): ValidationErrorInfo {
        val hash = (repository as PagedTransactionRepository).findBlockHash(errorHeight + 1)?.toHexReversed()
        val prevHash = repository.findBlockHash(errorHeight)?.toHexReversed()

        val compactBlock = downloader.compactBlockStore.findCompactBlock(errorHeight + 1)
        val expectedPrevHash = compactBlock?.prevHash?.toByteArray()?.toHexReversed()
        return ValidationErrorInfo(errorHeight, hash, expectedPrevHash, prevHash)
    }

    /**
     * Called for every noteworthy error.
     *
     * @return true when processing should continue. Return false when the error is unrecoverable
     * and all processing should halt and stop retrying.
     */
    private fun onProcessorError(throwable: Throwable): Boolean {
        return onProcessorErrorListener?.invoke(throwable) ?: true
    }

    private fun determineLowerBound(errorHeight: Int): Int {
        val offset = Math.min(MAX_REORG_SIZE, REWIND_DISTANCE * (consecutiveChainErrors.get() + 1))
        return Math.max(errorHeight - offset, lowerBoundHeight).also {
            twig("offset = min($MAX_REORG_SIZE, $REWIND_DISTANCE * (${consecutiveChainErrors.get() + 1})) = $offset")
            twig("lowerBound = max($errorHeight - $offset, $lowerBoundHeight) = $it")
        }
    }

    /**
     ￼* Poll on time boundaries. Per Issue #95, we want to avoid exposing computation time to a
     * network observer. Instead, we poll at regular time intervals that are large enough for all
     * computation to complete so no intervals are skipped. See 95 for more details.
     *
     * @param fastIntervalDesired currently not used but sometimes we want to poll quickly, such as
     * when we unexpectedly lose server connection or are waiting for an event to happen on the
     * chain. We can pass this desire along now and later figure out how to handle it, privately.
     ￼*/
    private fun calculatePollInterval(fastIntervalDesired: Boolean = false): Long {
        val interval = POLL_INTERVAL
        val now = System.currentTimeMillis()
        val deltaToNextInteral = interval - (now + interval).rem(interval)
        // twig("sleeping for ${deltaToNextInteral}ms from $now in order to wake at ${now + deltaToNextInteral}")
        return deltaToNextInteral
    }

    suspend fun calculateBirthdayHeight(): Int {
        var oldestTransactionHeight = 0
        try {
            oldestTransactionHeight = repository.receivedTransactions.first().last()?.minedHeight ?: lowerBoundHeight
            // to be safe adjust for reorgs (and generally a little cushion is good for privacy)
            // so we round down to the nearest 100 and then subtract 100 to ensure that the result is always at least 100 blocks away
            oldestTransactionHeight = ZcashSdk.MAX_REORG_SIZE.let { boundary ->
                oldestTransactionHeight.let { it - it.rem(boundary) - boundary }
            }
        } catch (t: Throwable) {
            twig("failed to calculate birthday due to: $t")
        }
        return maxOf(lowerBoundHeight, oldestTransactionHeight, rustBackend.network.saplingActivationHeight)
    }

    /**
     * Get the height of the last block that was downloaded by this processor.
     *
     * @return the last downloaded height reported by the downloader.
     */
    suspend fun getLastDownloadedHeight() = withContext(IO) {
        downloader.getLastDownloadedHeight()
    }

    /**
     * Get the height of the last block that was scanned by this processor.
     *
     * @return the last scanned height reported by the repository.
     */
    suspend fun getLastScannedHeight() = withContext(IO) {
        repository.lastScannedHeight()
    }

    /**
     * Get address corresponding to the given account for this wallet.
     *
     * @return the address of this wallet.
     */
    suspend fun getShieldedAddress(accountId: Int = 0) = withContext(IO) {
        repository.getAccount(accountId)?.rawShieldedAddress
            ?: throw InitializerException.MissingAddressException("shielded")
    }

    suspend fun getTransparentAddress(accountId: Int = 0) = withContext(IO) {
        repository.getAccount(accountId)?.rawTransparentAddress
            ?: throw InitializerException.MissingAddressException("transparent")
    }

    /**
     * Calculates the latest balance info. Defaults to the first account.
     *
     * @param accountIndex the account to check for balance info.
     *
     * @return an instance of WalletBalance containing information about available and total funds.
     */
    suspend fun getBalanceInfo(accountIndex: Int = 0): WalletBalance = withContext(IO) {
        twigTask("checking balance info", -1) {
            try {
                val balanceTotal = rustBackend.getBalance(accountIndex)
                twig("found total balance: $balanceTotal")
                val balanceAvailable = rustBackend.getVerifiedBalance(accountIndex)
                twig("found available balance: $balanceAvailable")
                WalletBalance(balanceTotal, balanceAvailable)
            } catch (t: Throwable) {
                twig("failed to get balance due to $t")
                throw RustLayerException.BalanceException(t)
            }
        }
    }

    suspend fun getUtxoCacheBalance(address: String): WalletBalance = withContext(IO) {
        rustBackend.getDownloadedUtxoBalance(address)
    }

    /**
     * Transmits the given state for this processor.
     */
    private suspend fun setState(newState: State) {
        _state.send(newState)
    }

    /**
     * Sealed class representing the various states of this processor.
     */
    sealed class State {
        /**
         * Marker interface for [State] instances that represent when the wallet is connected.
         */
        interface Connected

        /**
         * Marker interface for [State] instances that represent when the wallet is syncing.
         */
        interface Syncing

        /**
         * [State] for when the wallet is actively downloading compact blocks because the latest
         * block height available from the server is greater than what we have locally. We move out
         * of this state once our local height matches the server.
         */
        object Downloading : Connected, Syncing, State()

        /**
         * [State] for when the blocks that have been downloaded are actively being validated to
         * ensure that there are no gaps and that every block is chain-sequential to the previous
         * block, which determines whether a reorg has happened on our watch.
         */
        object Validating : Connected, Syncing, State()

        /**
         * [State] for when the blocks that have been downloaded are actively being decrypted.
         */
        object Scanning : Connected, Syncing, State()

        /**
         * [State] for when we are done decrypting blocks, for now.
         */
        class Scanned(val scannedRange: IntRange) : Connected, Syncing, State()

        /**
         * [State] for when transaction details are being retrieved. This typically means the wallet
         * has downloaded and scanned blocks and is now processing any transactions that were
         * discovered. Once a transaction is discovered, followup network requests are needed in
         * order to retrieve memos or outbound transaction information, like the recipient address.
         * The existing information we have about transactions is enhanced by the new information.
         */
        object Enhancing : Connected, Syncing, State()

        /**
         * [State] for when we have no connection to lightwalletd.
         */
        object Disconnected : State()

        /**
         * [State] for when [stop] has been called. For simplicity, processors should not be
         * restarted but they are not prevented from this behavior.
         */
        object Stopped : State()

        /**
         * [State] the initial state of the processor, once it is constructed.
         */
        object Initialized : State()
    }

    /**
     * Data class for holding detailed information about the processor.
     *
     * @param networkBlockHeight the latest block available to lightwalletd that may or may not be
     * downloaded by this wallet yet.
     * @param lastScannedHeight the height up to which the wallet last scanned. This determines
     * where the next scan will begin.
     * @param lastDownloadedHeight the last compact block that was successfully downloaded.
     *
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
        val lastScanRange: IntRange = 0..-1 // empty range
    ) {

        /**
         * Determines whether this instance has data.
         *
         * @return false when all values match their defaults.
         */
        val hasData get() = networkBlockHeight != -1 ||
            lastScannedHeight != -1 ||
            lastDownloadedHeight != -1 ||
            lastDownloadRange != 0..-1 ||
            lastScanRange != 0..-1

        /**
         * Determines whether this instance is actively downloading compact blocks.
         *
         * @return true when there are more than zero blocks remaining to download.
         */
        val isDownloading: Boolean get() = !lastDownloadRange.isEmpty() &&
            lastDownloadedHeight < lastDownloadRange.last

        /**
         * Determines whether this instance is actively scanning or validating compact blocks.
         *
         * @return true when downloading has completed and there are more than zero blocks remaining
         * to be scanned.
         */
        val isScanning: Boolean get() = !isDownloading &&
            !lastScanRange.isEmpty() &&
            lastScannedHeight < lastScanRange.last

        /**
         * The amount of scan progress from 0 to 100.
         */
        val scanProgress get() = when {
            lastScannedHeight <= -1 -> 0
            lastScanRange.isEmpty() -> 100
            lastScannedHeight >= lastScanRange.last -> 100
            else -> {
                // when lastScannedHeight == lastScanRange.first, we have scanned one block, thus the offsets
                val blocksScanned = (lastScannedHeight - lastScanRange.first + 1).coerceAtLeast(0)
                // we scan the range inclusively so 100..100 is one block to scan, thus the offset
                val numberOfBlocks = lastScanRange.last - lastScanRange.first + 1
                // take the percentage then convert and round
                ((blocksScanned.toFloat() / numberOfBlocks) * 100.0f).let { percent ->
                    percent.coerceAtMost(100.0f).roundToInt()
                }
            }
        }
    }

    data class ValidationErrorInfo(
        val errorHeight: Int,
        val hash: String?,
        val expectedPrevHash: String?,
        val actualPrevHash: String?
    )

    //
    // Helper Extensions
    //

    private fun Service.LightdInfo.matchingConsensusBranchId(clientBranch: String): Boolean {
        return consensusBranchId.equals(clientBranch, true)
    }

    private fun Service.LightdInfo.matchingNetwork(network: String): Boolean {
        fun String.toId() = toLowerCase(Locale.US).run {
            when {
                contains("main") -> "mainnet"
                contains("test") -> "testnet"
                else -> this
            }
        }
        return chainName.toId() == network.toId()
    }

    /**
     * Log the mutex in great detail just in case we need it for troubleshooting deadlock.
     */
    private suspend inline fun <T> Mutex.withLockLogged(name: String, block: () -> T): T {
        twig("$name MUTEX: acquiring lock...", -1)
        this.withLock {
            twig("$name MUTEX: ...lock acquired!", -1)
            return block().also {
                twig("$name MUTEX: releasing lock", -1)
            }
        }
        twig("$name MUTEX: withLock complete", -1)
    }

    companion object {
        const val ERROR_CODE_NONE = -1
        const val ERROR_CODE_RECONNECT = 20
        const val ERROR_CODE_FAILED_ENHANCE = 40
    }
}
