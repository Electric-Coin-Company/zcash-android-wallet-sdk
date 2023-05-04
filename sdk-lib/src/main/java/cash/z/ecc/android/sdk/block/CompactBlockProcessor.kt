package cash.z.ecc.android.sdk.block

import androidx.annotation.VisibleForTesting
import cash.z.ecc.android.sdk.BuildConfig
import cash.z.ecc.android.sdk.annotation.OpenForTesting
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException.EnhanceTransactionError.EnhanceTxDecryptError
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException.EnhanceTransactionError.EnhanceTxDownloadError
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException.MismatchedNetwork
import cash.z.ecc.android.sdk.exception.InitializeException
import cash.z.ecc.android.sdk.exception.LightWalletException
import cash.z.ecc.android.sdk.exception.RustLayerException
import cash.z.ecc.android.sdk.ext.BatchMetrics
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.ZcashSdk.MAX_BACKOFF_INTERVAL
import cash.z.ecc.android.sdk.ext.ZcashSdk.POLL_INTERVAL
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.block.CompactBlockDownloader
import cash.z.ecc.android.sdk.internal.ext.retryUpTo
import cash.z.ecc.android.sdk.internal.ext.retryWithBackoff
import cash.z.ecc.android.sdk.internal.ext.toHexReversed
import cash.z.ecc.android.sdk.internal.isNullOrEmpty
import cash.z.ecc.android.sdk.internal.length
import cash.z.ecc.android.sdk.internal.model.DbTransactionOverview
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.model.ext.from
import cash.z.ecc.android.sdk.internal.model.ext.toBlockHeight
import cash.z.ecc.android.sdk.internal.repository.DerivedDataRepository
import cash.z.ecc.android.sdk.jni.Backend
import cash.z.ecc.android.sdk.jni.createAccountAndGetSpendingKey
import cash.z.ecc.android.sdk.jni.getBalance
import cash.z.ecc.android.sdk.jni.getBranchIdForHeight
import cash.z.ecc.android.sdk.jni.getCurrentAddress
import cash.z.ecc.android.sdk.jni.getDownloadedUtxoBalance
import cash.z.ecc.android.sdk.jni.getNearestRewindHeight
import cash.z.ecc.android.sdk.jni.getVerifiedBalance
import cash.z.ecc.android.sdk.jni.listTransparentReceivers
import cash.z.ecc.android.sdk.jni.rewindToHeight
import cash.z.ecc.android.sdk.jni.validateCombinedChainOrErrorBlockHeight
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockBatch
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.PercentDecimal
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.ext.BenchmarkingExt
import co.electriccoin.lightwallet.client.fixture.BenchmarkingBlockRangeFixture
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.GetAddressUtxosReplyUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.Response
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days

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
@Suppress("TooManyFunctions", "LargeClass")
class CompactBlockProcessor internal constructor(
    val downloader: CompactBlockDownloader,
    private val repository: DerivedDataRepository,
    private val rustBackend: Backend,
    minimumHeight: BlockHeight = rustBackend.network.saplingActivationHeight
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
    var onChainErrorListener: ((errorHeight: BlockHeight, rewindHeight: BlockHeight) -> Any)? = null

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
    private val lowerBoundHeight: BlockHeight = BlockHeight(
        max(
            rustBackend.network.saplingActivationHeight.value,
            minimumHeight.value - MAX_REORG_SIZE
        )
    )

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Initialized)
    private val _progress = MutableStateFlow(PercentDecimal.ZERO_PERCENT)
    private val _processorInfo = MutableStateFlow(ProcessorInfo(null, null, null))
    private val _networkHeight = MutableStateFlow<BlockHeight?>(null)
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
     * The zcash network that is being processed. Either Testnet or Mainnet.
     */
    val network = rustBackend.network

    /**
     * The flow of state values so that a wallet can monitor the state of this class without needing
     * to poll.
     */
    val state = _state.asStateFlow()

    /**
     * The flow of progress values so that a wallet can monitor how much downloading remains
     * without needing to poll.
     */
    val progress = _progress.asStateFlow()

    /**
     * The flow of detailed processorInfo like the range of blocks that shall be downloaded and
     * scanned. This gives the wallet a lot of insight into the work of this processor.
     */
    val processorInfo = _processorInfo.asStateFlow()

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
    @Suppress("LongMethod")
    suspend fun start() {
        verifySetup()

        updateBirthdayHeight()

        // Clear any undeleted left over block files from previous sync attempts
        deleteAllBlockFiles(
            downloader = downloader,
            lastKnownHeight = getLastScannedHeight(repository)
        )

        Twig.debug { "setup verified. processor starting" }

        // using do/while makes it easier to execute exactly one loop which helps with testing this processor quickly
        // (because you can start and then immediately set isStopped=true to always get precisely one loop)
        do {
            retryWithBackoff(::onProcessorError, maxDelayMillis = MAX_BACKOFF_INTERVAL) {
                val result = processingMutex.withLockLogged("processNewBlocks") {
                    processNewBlocks()
                }
                // immediately process again after failures in order to download new blocks right away
                when (result) {
                    BlockProcessingResult.Reconnecting -> {
                        val napTime = calculatePollInterval(true)
                        Twig.debug {
                            "Unable to process new blocks because we are disconnected! Attempting to " +
                                "reconnect in ${napTime}ms"
                        }
                        delay(napTime)
                    }

                    BlockProcessingResult.NoBlocksToProcess, BlockProcessingResult.FailedEnhance -> {
                        val noWorkDone = _processorInfo.value.lastSyncRange?.isEmpty() ?: true
                        val summary = if (noWorkDone) {
                            "Nothing to process: no new blocks to sync"
                        } else {
                            "Done processing blocks"
                        }
                        consecutiveChainErrors.set(0)
                        val napTime = calculatePollInterval()
                        Twig.debug {
                            "$summary${
                                if (result == BlockProcessingResult.FailedEnhance) {
                                    " (but there were" +
                                        " enhancement errors! We ignore those, for now. Memos in this block range are" +
                                        " probably missing! This will be improved in a future release.)"
                                } else {
                                    ""
                                }
                            }! Sleeping" +
                                " for ${napTime}ms (latest height: ${_processorInfo.value.networkBlockHeight})."
                        }
                        delay(napTime)
                    }

                    is BlockProcessingResult.FailedDeleteBlocks -> {
                        Twig.error {
                            "Failed to delete temporary blocks files from the device disk. It will be retried on the" +
                                " next time, while downloading new blocks."
                        }
                        checkErrorResult(result.failedAtHeight)
                    }

                    is BlockProcessingResult.FailedDownloadBlocks -> {
                        Twig.error { "Failed while downloading blocks at height: ${result.failedAtHeight}" }
                        checkErrorResult(result.failedAtHeight)
                    }

                    is BlockProcessingResult.FailedValidateBlocks -> {
                        Twig.error { "Failed while validating blocks at height: ${result.failedAtHeight}" }
                        checkErrorResult(result.failedAtHeight)
                    }

                    is BlockProcessingResult.FailedScanBlocks -> {
                        Twig.error { "Failed while scanning blocks at height: ${result.failedAtHeight}" }
                        checkErrorResult(result.failedAtHeight)
                    }

                    is BlockProcessingResult.Success -> {
                        // Do nothing. We are done.
                    }

                    is BlockProcessingResult.DownloadSuccess -> {
                        // Do nothing. Syncing of blocks is in progress.
                    }
                }
            }
        } while (_state.value !is State.Stopped)
        Twig.debug { "processor complete" }
        stop()
    }

    suspend fun checkErrorResult(failedHeight: BlockHeight) {
        if (consecutiveChainErrors.get() >= RETRIES) {
            val errorMessage = "ERROR: unable to resolve reorg at height $failedHeight after " +
                "${consecutiveChainErrors.get()} correction attempts!"
            fail(CompactBlockProcessorException.FailedReorgRepair(errorMessage))
        } else {
            handleChainError(failedHeight)
        }
        consecutiveChainErrors.getAndIncrement()
    }

    /**
     * Sets the state to [State.Stopped], which causes the processor loop to exit.
     */
    suspend fun stop() {
        runCatching {
            setState(State.Stopped)
            downloader.stop()
        }
    }

    /**
     * Stop processing and throw an error.
     */
    private suspend fun fail(error: Throwable) {
        stop()
        Twig.debug { "${error.message}" }
        throw error
    }

    private suspend fun processNewBlocks(): BlockProcessingResult {
        Twig.debug { "Beginning to process new blocks (with lower bound: $lowerBoundHeight)..." }

        return if (!updateRanges()) {
            Twig.debug { "Disconnection detected! Attempting to reconnect!" }
            setState(State.Disconnected)
            downloader.lightWalletClient.reconnect()
            BlockProcessingResult.Reconnecting
        } else if (_processorInfo.value.lastSyncRange.isNullOrEmpty()) {
            setState(State.Synced(_processorInfo.value.lastSyncRange))
            BlockProcessingResult.NoBlocksToProcess
        } else {
            val syncRange = if (BenchmarkingExt.isBenchmarking()) {
                // We inject a benchmark test blocks range at this point to process only a restricted range of
                // blocks for a more reliable benchmark results.
                val benchmarkBlockRange = BenchmarkingBlockRangeFixture.new().let {
                    // Convert range of Longs to range of BlockHeights
                    BlockHeight.new(ZcashNetwork.Mainnet, it.start)..(
                        BlockHeight.new(ZcashNetwork.Mainnet, it.endInclusive)
                        )
                }
                benchmarkBlockRange
            } else {
                _processorInfo.value.lastSyncRange!!
            }

            syncBlocksAndEnhanceTransactions(
                syncRange = syncRange,
                withDownload = true
            )
        }
    }

    @Suppress("ReturnCount")
    private suspend fun syncBlocksAndEnhanceTransactions(
        syncRange: ClosedRange<BlockHeight>,
        withDownload: Boolean
    ): BlockProcessingResult {
        _state.value = State.Syncing

        // Sync
        var syncResult: BlockProcessingResult = BlockProcessingResult.Success
        syncNewBlocks(
            backend = rustBackend,
            downloader = downloader,
            repository = repository,
            network = network,
            syncRange = syncRange,
            withDownload = withDownload
        ).collect { syncProgress ->
            _progress.value = syncProgress.percentage
            updateProgress(lastSyncedHeight = syncProgress.lastSyncedHeight)

            // Cancel collecting in case of any unwanted state comes
            if (syncProgress.result != BlockProcessingResult.Success) {
                syncResult = syncProgress.result
                return@collect
            }
        }
        if (syncResult != BlockProcessingResult.Success) {
            // Remove persisted but not validated and scanned blocks in case of any failure
            val lastScannedHeight = getLastScannedHeight(repository)
            downloader.rewindToHeight(lastScannedHeight)
            deleteAllBlockFiles(
                downloader = downloader,
                lastKnownHeight = lastScannedHeight
            )

            return syncResult
        }

        // Enhance
        val enhanceResult = enhanceTransactionDetails(syncRange)

        if (enhanceResult != BlockProcessingResult.Success ||
            enhanceResult != BlockProcessingResult.NoBlocksToProcess
        ) {
            return enhanceResult
        }

        return BlockProcessingResult.Success
    }

    sealed class BlockProcessingResult {
        object NoBlocksToProcess : BlockProcessingResult()
        object Success : BlockProcessingResult()
        data class DownloadSuccess(val downloadedBlocks: List<JniBlockMeta>?) : BlockProcessingResult()
        object Reconnecting : BlockProcessingResult()
        data class FailedDownloadBlocks(val failedAtHeight: BlockHeight) : BlockProcessingResult()
        data class FailedScanBlocks(val failedAtHeight: BlockHeight) : BlockProcessingResult()
        data class FailedValidateBlocks(val failedAtHeight: BlockHeight) : BlockProcessingResult()
        data class FailedDeleteBlocks(val failedAtHeight: BlockHeight) : BlockProcessingResult()
        object FailedEnhance : BlockProcessingResult()
    }

    /**
     * Gets the latest range info and then uses that initialInfo to update (and transmit)
     * the scan/download ranges that require processing.
     *
     * @return true when the update succeeds.
     */
    private suspend fun updateRanges(): Boolean {
        // This fetches the latest height each time this method is called, which can be very inefficient
        // when downloading all of the blocks from the server
        val networkBlockHeight = run {
            val networkBlockHeightUnsafe =
                when (val response = downloader.getLatestBlockHeight()) {
                    is Response.Success -> response.result
                    else -> null
                }

            runCatching { networkBlockHeightUnsafe?.toBlockHeight(network) }.getOrNull()
        } ?: return false

        // If we find out that we previously downloaded, but not validated and scanned persisted blocks, we need
        // to rewind the blocks above the last scanned height first.
        val lastScannedHeight = getLastScannedHeight(repository)
        val lastDownloadedHeight = getLastDownloadedHeight(downloader).let {
            BlockHeight.new(
                network,
                max(
                    it?.value ?: 0,
                    lowerBoundHeight.value - 1
                )
            )
        }
        val lastSyncedHeight = if (lastDownloadedHeight.value - lastScannedHeight.value > 0) {
            Twig.verbose {
                "Clearing blocks of last persisted batch within the last scanned height " +
                    "$lastScannedHeight and last download height $lastDownloadedHeight, as all these blocks " +
                    "possibly haven't been validated and scanned in the previous blocks sync attempt."
            }
            downloader.rewindToHeight(lastScannedHeight)
            lastScannedHeight
        } else {
            lastDownloadedHeight
        }

        updateProgress(
            networkBlockHeight = networkBlockHeight,
            lastSyncedHeight = lastSyncedHeight,
            lastSyncRange = lastSyncedHeight + 1..networkBlockHeight
        )

        return true
    }

    private suspend fun enhanceTransactionDetails(lastScanRange: ClosedRange<BlockHeight>?): BlockProcessingResult {
        if (lastScanRange == null) {
            return BlockProcessingResult.NoBlocksToProcess
        }

        Twig.debug { "Enhancing transaction details for blocks $lastScanRange" }
        setState(State.Enhancing)
        @Suppress("TooGenericExceptionCaught")
        return try {
            val newTxs = repository.findNewTransactions(lastScanRange)
            if (newTxs.isEmpty()) {
                Twig.debug { "no new transactions found in $lastScanRange" }
            } else {
                Twig.debug { "enhancing ${newTxs.size} transaction(s)!" }
                // if the first transaction has been added
                if (newTxs.size.toLong() == repository.getTransactionCount()) {
                    Twig.debug { "Encountered the first transaction. This changes the birthday height!" }
                    updateBirthdayHeight()
                }
            }

            newTxs.filter { it.minedHeight != null }.onEach { newTransaction ->
                enhance(newTransaction)
            }
            Twig.debug { "Done enhancing transaction details" }
            BlockProcessingResult.Success
        } catch (t: Throwable) {
            Twig.debug { "Failed to enhance due to: ${t.message} caused by: ${t.cause}" }
            BlockProcessingResult.FailedEnhance
        }
    }
    // TODO [#683]: we still need a way to identify those transactions that failed to be enhanced
    // TODO [#683]: https://github.com/zcash/zcash-android-wallet-sdk/issues/683

    private suspend fun enhance(transaction: DbTransactionOverview) {
        transaction.minedHeight?.let { minedHeight ->
            enhanceHelper(transaction.id, transaction.rawId.byteArray, minedHeight)
        }
    }

    private suspend fun enhanceHelper(id: Long, rawTransactionId: ByteArray, minedHeight: BlockHeight) {
        Twig.debug { "START: enhancing transaction (id:$id  block:$minedHeight)" }

        when (val response = downloader.fetchTransaction(rawTransactionId)) {
            is Response.Success -> {
                runCatching {
                    Twig.debug { "decrypting and storing transaction (id:$id  block:$minedHeight)" }
                    rustBackend.decryptAndStoreTransaction(response.result.data)
                }.onSuccess {
                    Twig.debug { "DONE: enhancing transaction (id:$id  block:$minedHeight)" }
                }.onFailure { error ->
                    onProcessorError(EnhanceTxDecryptError(minedHeight, error))
                }
            }

            is Response.Failure -> {
                onProcessorError(EnhanceTxDownloadError(minedHeight, response.toThrowable()))
            }
        }
    }

    /**
     * Confirm that the wallet data is properly setup for use.
     */
    // Need to refactor this to be less ugly and more testable
    @Suppress("NestedBlockDepth")
    private suspend fun verifySetup() {
        // verify that the data is initialized
        val error = if (!repository.isInitialized()) {
            CompactBlockProcessorException.Uninitialized
        } else if (repository.getAccountCount() == 0) {
            CompactBlockProcessorException.NoAccount
        } else {
            // verify that the server is correct

            // How do we handle network connection issues?

            downloader.getServerInfo()?.let { info ->
                val serverBlockHeight =
                    runCatching { info.blockHeightUnsafe.toBlockHeight(network) }.getOrNull()

                if (null == serverBlockHeight) {
                    // TODO Better signal network connection issue
                    CompactBlockProcessorException.BadBlockHeight(info.blockHeightUnsafe)
                } else {
                    val clientBranch = "%x".format(
                        Locale.ROOT,
                        rustBackend.getBranchIdForHeight(serverBlockHeight)
                    )
                    val network = rustBackend.network.networkName

                    if (!clientBranch.equals(info.consensusBranchId, true)) {
                        MismatchedNetwork(
                            clientNetwork = network,
                            serverNetwork = info.chainName
                        )
                    } else if (!info.matchingNetwork(network)) {
                        MismatchedNetwork(
                            clientNetwork = network,
                            serverNetwork = info.chainName
                        )
                    } else {
                        null
                    }
                }
            }
        }

        if (error != null) {
            Twig.debug { "Validating setup prior to scanning . . . ISSUE FOUND! - ${error.javaClass.simpleName}" }
            // give listener a chance to override
            if (onSetupErrorListener?.invoke(error) != true) {
                throw error
            } else {
                Twig.debug {
                    "Warning: An ${error::class.java.simpleName} was encountered while verifying setup but " +
                        "it was ignored by the onSetupErrorHandler. Ignoring message: ${error.message}"
                }
            }
        }
    }

    private suspend fun updateBirthdayHeight() {
        @Suppress("TooGenericExceptionCaught")
        try {
            val betterBirthday = calculateBirthdayHeight()
            if (betterBirthday > birthdayHeight) {
                Twig.debug { "Better birthday found! Birthday height updated from $birthdayHeight to $betterBirthday" }
                _birthdayHeight.value = betterBirthday
            }
        } catch (e: Throwable) {
            Twig.debug(e) { "Warning: updating the birthday height failed" }
        }
    }

    var failedUtxoFetches = 0

    @Suppress("MagicNumber", "LongMethod")
    internal suspend fun refreshUtxos(account: Account, startHeight: BlockHeight): Int {
        Twig.debug { "Checking for UTXOs above height $startHeight" }
        var count = 0
        // TODO [683]: cleanup the way that we prevent this from running excessively
        //       For now, try for about 3 blocks per app launch. If the service fails it is
        //       probably disabled on ligthtwalletd, so then stop trying until the next app launch.
        // TODO [#683]: https://github.com/zcash/zcash-android-wallet-sdk/issues/683
        if (failedUtxoFetches < 9) { // there are 3 attempts per block
            @Suppress("TooGenericExceptionCaught")
            try {
                retryUpTo(3) {
                    val tAddresses = rustBackend.listTransparentReceivers(account)

                    downloader.lightWalletClient.fetchUtxos(
                        tAddresses,
                        BlockHeightUnsafe.from(startHeight)
                    ).onEach { response ->
                        when (response) {
                            is Response.Success -> {
                                Twig.verbose { "Downloading UTXO at height: ${response.result.height} succeeded." }
                            }

                            is Response.Failure -> {
                                Twig.warn {
                                    "Downloading UTXO from height:" +
                                        " $startHeight failed with: ${response.description}."
                                }
                                throw LightWalletException.FetchUtxosException(
                                    response.code,
                                    response.description,
                                    response.toThrowable()
                                )
                            }
                        }
                    }
                        .filterIsInstance<Response.Success<GetAddressUtxosReplyUnsafe>>()
                        .map { response ->
                            response.result
                        }
                        .onCompletion {
                            if (it != null) {
                                Twig.debug { "UTXOs from height $startHeight failed to download with: $it" }
                            } else {
                                Twig.debug { "All UTXOs from height $startHeight fetched successfully" }
                            }
                        }.collect { utxo ->
                            Twig.debug { "Fetched UTXO with txid: ${utxo.txid}" }
                            val processResult = processUtxoResult(utxo)
                            if (processResult) {
                                count++
                            }
                        }
                }
            } catch (e: Throwable) {
                failedUtxoFetches++
                Twig.debug {
                    "Warning: Fetching UTXOs is repeatedly failing! We will only try about " +
                        "${(9 - failedUtxoFetches + 2) / 3} more times then give up for this session. " +
                        "Exception message: ${e.message}, caused by: ${e.cause}."
                }
            }
        } else {
            Twig.debug {
                "Warning: gave up on fetching UTXOs for this session. It seems to unavailable on " +
                    "lightwalletd."
            }
        }

        return count
    }

    /**
     * @return True in case of the UTXO processed successfully, false otherwise
     */
    internal suspend fun processUtxoResult(utxo: GetAddressUtxosReplyUnsafe): Boolean {
        // TODO(str4d): We no longer clear UTXOs here, as rustBackend.putUtxo now uses an upsert instead of an insert.
        //  This means that now-spent UTXOs would previously have been deleted, but now are left in the database (like
        //  shielded notes). Due to the fact that the lightwalletd query only returns _current_ UTXOs, we don't learn
        //  about recently-spent UTXOs here, so the transparent balance does not get updated here. Instead, when a
        //  received shielded note is "enhanced" by downloading the full transaction, we mark any UTXOs spent in that
        //  transaction as spent in the database. This relies on two current properties: UTXOs are only ever spent in
        //  shielding transactions, and at least one shielded note from each shielding transaction is always enhanced.
        //  However, for greater reliability, we may want to alter the Data Access API to support "inferring spentness"
        //  from what is _not_ returned as a UTXO, or alternatively fetch TXOs from lightwalletd instead of just UTXOs.
        Twig.debug { "Found UTXO at height ${utxo.height.toInt()} with ${utxo.valueZat} zatoshi" }
        @Suppress("TooGenericExceptionCaught")
        return try {
            // TODO [#920]: Tweak RustBackend public APIs to have void return values.
            // TODO [#920]: Thus, we don't need to check the boolean result of this call until fixed.
            // TODO [#920]: https://github.com/zcash/zcash-android-wallet-sdk/issues/920
            rustBackend.putUtxo(
                utxo.address,
                utxo.txid,
                utxo.index,
                utxo.script,
                utxo.valueZat,
                BlockHeight(utxo.height)
            )
            true
        } catch (t: Throwable) {
            Twig.debug {
                "Warning: Ignoring transaction at height ${utxo.height} @ index ${utxo.index} because " +
                    "it already exists. Exception message: ${t.message}, caused by: ${t.cause}."
            }
            // TODO [#683]: more accurately track the utxos that were skipped (in theory, this could fail for other
            //  reasons)
            // TODO [#683]: https://github.com/zcash/zcash-android-wallet-sdk/issues/683
            false
        }
    }

    companion object {
        /**
         * Default attempts at retrying.
         */
        internal const val RETRIES = 5

        /**
         * The theoretical maximum number of blocks in a reorg, due to other bottlenecks in the protocol design.
         */
        internal const val MAX_REORG_SIZE = 100

        /**
         * Default size of batches of blocks to request from the compact block service. Then it's also used as a default
         * size of batches of blocks to scan via librustzcash. The smaller this number the more granular information can
         * be provided about scan state. Unfortunately, it may also lead to a lot of overhead during scanning.
         */
        internal const val SYNC_BATCH_SIZE = 10

        /**
         * Default number of blocks to rewind when a chain reorg is detected. This should be large enough to recover
         * from the reorg but smaller than the theoretical max reorg size of 100.
         */
        internal const val REWIND_DISTANCE = 10

        /**
         * Requests, processes and persists all blocks from the given range.
         *
         * @param backend the Rust backend component
         * @param downloader the compact block downloader component
         * @param repository the derived data repository component
         * @param network the network in which the sync mechanism operates
         * @param syncRange the range of blocks to download
         * @param withDownload the flag indicating whether the blocks should also be downloaded and processed, or
         * processed existing blocks

         * @return Flow of BatchSyncProgress sync results
         */
        @VisibleForTesting
        @Suppress("LongParameterList", "LongMethod")
        internal suspend fun syncNewBlocks(
            backend: Backend,
            downloader: CompactBlockDownloader,
            repository: DerivedDataRepository,
            network: ZcashNetwork,
            syncRange: ClosedRange<BlockHeight>,
            withDownload: Boolean
        ): Flow<BatchSyncProgress> = flow {
            if (syncRange.isEmpty()) {
                Twig.debug { "No blocks to sync" }
                emit(
                    BatchSyncProgress(
                        percentage = PercentDecimal.ONE_HUNDRED_PERCENT,
                        lastSyncedHeight = getLastScannedHeight(repository),
                        result = BlockProcessingResult.Success
                    )
                )
            } else {
                Twig.debug { "Syncing blocks in range $syncRange" }

                val batches = getBatchedBlockList(syncRange, network)

                batches.asFlow().map {
                    Twig.debug { "Syncing process starts for batch: $it" }

                    // Run downloading stage
                    SyncStageResult(
                        batch = it,
                        stageResult = if (withDownload) {
                            downloadBatchOfBlocks(
                                downloader = downloader,
                                batch = it
                            )
                        } else {
                            BlockProcessingResult.DownloadSuccess(null)
                        }
                    )
                }.buffer(1).map { downloadStageResult ->
                    Twig.debug { "Download stage done with result: $downloadStageResult" }

                    if (downloadStageResult.stageResult !is BlockProcessingResult.DownloadSuccess) {
                        // In case of any failure, we just propagate the result
                        downloadStageResult
                    } else {
                        // Enrich batch model with fetched blocks. It's useful for later blocks deletion
                        downloadStageResult.batch.blocks = downloadStageResult.stageResult.downloadedBlocks

                        // Run validation stage
                        SyncStageResult(
                            downloadStageResult.batch,
                            validateBatchOfBlocks(
                                backend = backend,
                                batch = downloadStageResult.batch
                            )
                        )
                    }
                }.map { validateResult ->
                    Twig.debug { "Validation stage done with result: $validateResult" }

                    if (validateResult.stageResult != BlockProcessingResult.Success) {
                        validateResult
                    } else {
                        // Run scanning stage
                        SyncStageResult(
                            validateResult.batch,
                            scanBatchOfBlocks(
                                backend = backend,
                                batch = validateResult.batch
                            )
                        )
                    }
                }.map { scanResult ->
                    Twig.debug { "Scan stage done with result: $scanResult" }

                    if (scanResult.stageResult != BlockProcessingResult.Success) {
                        scanResult
                    } else {
                        // Run deletion stage
                        SyncStageResult(
                            scanResult.batch,
                            deleteFilesOfBatchOfBlocks(
                                downloader = downloader,
                                batch = scanResult.batch
                            )
                        )
                    }
                }.onEach { deleteResult ->
                    Twig.debug { "Deletion stage done with result: $deleteResult" }

                    emit(
                        BatchSyncProgress(
                            percentage = PercentDecimal(deleteResult.batch.index / batches.size.toFloat()),
                            lastSyncedHeight = getLastScannedHeight(repository),
                            result = deleteResult.stageResult
                        )
                    )

                    Twig.debug { "All sync stages done for the batch: ${deleteResult.batch}" }
                }.takeWhile { continuousResult ->
                    continuousResult.stageResult == BlockProcessingResult.Success
                }.collect()
            }
        }

        private fun getBatchedBlockList(
            syncRange: ClosedRange<BlockHeight>,
            network: ZcashNetwork
        ): List<BlockBatch> {
            val missingBlockCount = syncRange.endInclusive.value - syncRange.start.value + 1
            val batchCount = (
                missingBlockCount / SYNC_BATCH_SIZE +
                    (if (missingBlockCount.rem(SYNC_BATCH_SIZE) == 0L) 0 else 1)
                )

            Twig.debug {
                "Found $missingBlockCount missing blocks, syncing in $batchCount batches of $SYNC_BATCH_SIZE..."
            }

            var start = syncRange.start
            return mutableListOf<BlockBatch>().apply {
                for (index in 1..batchCount) {
                    val end = BlockHeight.new(
                        network,
                        min(
                            (syncRange.start.value + (index * SYNC_BATCH_SIZE)) - 1,
                            syncRange.endInclusive.value
                        )
                    ) // subtract 1 on the first value because the range is inclusive

                    add(BlockBatch(index, start..end))
                    start = end + 1
                }
            }
        }

        /**
         * Request and download all blocks in the given range and persist them locally for processing, later.
         *
         * @param batch the batch of blocks to download.
         */
        @VisibleForTesting
        @Throws(CompactBlockProcessorException.FailedDownload::class)
        @Suppress("MagicNumber")
        internal suspend fun downloadBatchOfBlocks(
            downloader: CompactBlockDownloader,
            batch: BlockBatch
        ): BlockProcessingResult {
            var downloadedBlocks = listOf<JniBlockMeta>()
            retryUpTo(RETRIES, { CompactBlockProcessorException.FailedDownload(it) }) { failedAttempts ->
                if (failedAttempts == 0) {
                    Twig.verbose { "Starting to download batch $batch" }
                } else {
                    Twig.verbose { "Retrying to download batch $batch after $failedAttempts failure(s)..." }
                }

                downloadedBlocks = downloader.downloadBlockRange(batch.range)
            }
            Twig.verbose { "Successfully downloaded batch: $batch of $downloadedBlocks blocks" }

            return if (downloadedBlocks.isNotEmpty()) {
                BlockProcessingResult.DownloadSuccess(downloadedBlocks)
            } else {
                BlockProcessingResult.FailedDownloadBlocks(batch.range.start)
            }
        }

        @VisibleForTesting
        internal suspend fun validateBatchOfBlocks(batch: BlockBatch, backend: Backend): BlockProcessingResult {
            Twig.verbose { "Starting to validate batch $batch" }

            val result = backend.validateCombinedChainOrErrorBlockHeight(batch.range.length())

            return if (null == result) {
                Twig.verbose { "Successfully validated batch $batch" }
                BlockProcessingResult.Success
            } else {
                BlockProcessingResult.FailedValidateBlocks(result)
            }
        }

        @VisibleForTesting
        internal suspend fun scanBatchOfBlocks(batch: BlockBatch, backend: Backend): BlockProcessingResult {
            val scanResult = backend.scanBlocks(batch.range.length())
            return if (scanResult) {
                Twig.verbose { "Successfully scanned batch $batch" }
                BlockProcessingResult.Success
            } else {
                BlockProcessingResult.FailedScanBlocks(batch.range.start)
            }
        }

        @VisibleForTesting
        internal suspend fun deleteAllBlockFiles(
            downloader: CompactBlockDownloader,
            lastKnownHeight: BlockHeight
        ): BlockProcessingResult {
            Twig.verbose { "Starting to delete all temporary block files" }
            return if (downloader.compactBlockRepository.deleteAllCompactBlockFiles()) {
                Twig.verbose { "Successfully deleted all temporary block files" }
                BlockProcessingResult.Success
            } else {
                BlockProcessingResult.FailedDeleteBlocks(lastKnownHeight)
            }
        }

        @VisibleForTesting
        internal suspend fun deleteFilesOfBatchOfBlocks(batch: BlockBatch, downloader: CompactBlockDownloader):
            BlockProcessingResult {
            Twig.verbose { "Starting to delete temporary block files from batch: $batch" }

            return batch.blocks?.let { blocks ->
                val deleted = downloader.compactBlockRepository.deleteCompactBlockFiles(blocks)
                if (deleted) {
                    Twig.verbose { "Successfully deleted all temporary batched block files" }
                    BlockProcessingResult.Success
                } else {
                    BlockProcessingResult.FailedDeleteBlocks(batch.range.start)
                }
            } ?: BlockProcessingResult.Success
        }

        /**
         * Get the height of the last block that was scanned by this processor.
         *
         * @return the last scanned height reported by the repository.
         */
        @VisibleForTesting
        internal suspend fun getLastScannedHeight(repository: DerivedDataRepository) =
            repository.lastScannedHeight()

        /**
         * Get the height of the last block that was downloaded by this processor.
         *
         * @return the last downloaded height reported by the downloader.
         */
        internal suspend fun getLastDownloadedHeight(downloader: CompactBlockDownloader) =
            downloader.getLastDownloadedHeight()

        // CompactBlockProcessor is the wrong place for this, but it's where all the other APIs that need
        //  access to the RustBackend live. This should be refactored.
        internal suspend fun createAccount(rustBackend: Backend, seed: ByteArray): UnifiedSpendingKey =
            rustBackend.createAccountAndGetSpendingKey(seed)

        /**
         * Get the current unified address for the given wallet account.
         *
         * @return the current unified address of this account.
         */
        internal suspend fun getCurrentAddress(rustBackend: Backend, account: Account) =
            rustBackend.getCurrentAddress(account)

        /**
         * Get the legacy Sapling address corresponding to the current unified address for the given wallet account.
         *
         * @return a Sapling address.
         */
        internal suspend fun getLegacySaplingAddress(rustBackend: Backend, account: Account) =
            rustBackend.getSaplingReceiver(
                rustBackend.getCurrentAddress(account)
            )
                ?: throw InitializeException.MissingAddressException("legacy Sapling")

        /**
         * Get the legacy transparent address corresponding to the current unified address for the given wallet account.
         *
         * @return a transparent address.
         */
        internal suspend fun getTransparentAddress(rustBackend: Backend, account: Account) =
            rustBackend.getTransparentReceiver(
                rustBackend.getCurrentAddress(account)
            )
                ?: throw InitializeException.MissingAddressException("legacy transparent")
    }

    /**
     * Emit an instance of processorInfo, corresponding to the provided data.
     *
     * @param networkBlockHeight the latest block available to lightwalletd that may or may not be
     * downloaded by this wallet yet.
     * @param lastSyncedHeight the height up to which the wallet last synced. This determines
     * where the next sync will begin.
     * @param lastSyncRange the inclusive range to sync. This represents what we most recently
     * wanted to sync. In most cases, it will be an invalid range because we'd like to sync blocks
     * that we don't yet have.
     */
    private fun updateProgress(
        networkBlockHeight: BlockHeight? = _processorInfo.value.networkBlockHeight,
        lastSyncedHeight: BlockHeight? = _processorInfo.value.lastSyncedHeight,
        lastSyncRange: ClosedRange<BlockHeight>? = _processorInfo.value.lastSyncRange,
    ) {
        _networkHeight.value = networkBlockHeight
        _processorInfo.value = ProcessorInfo(
            networkBlockHeight = networkBlockHeight,
            lastSyncedHeight = lastSyncedHeight,
            lastSyncRange = lastSyncRange
        )
    }

    private suspend fun handleChainError(errorHeight: BlockHeight) {
        // TODO [#683]: consider an error object containing hash information
        // TODO [#683]: https://github.com/zcash/zcash-android-wallet-sdk/issues/683
        printValidationErrorInfo(errorHeight)
        determineLowerBound(errorHeight).let { lowerBound ->
            Twig.debug { "handling chain error at $errorHeight by rewinding to block $lowerBound" }
            onChainErrorListener?.invoke(errorHeight, lowerBound)
            rewindToNearestHeight(lowerBound, true)
        }
    }

    suspend fun getNearestRewindHeight(height: BlockHeight): BlockHeight {
        // TODO [#683]: add a concept of original checkpoint height to the processor. For now, derive it
        //  add one because we already have the checkpoint. Add one again because we delete ABOVE the block
        // TODO [#683]: https://github.com/zcash/zcash-android-wallet-sdk/issues/683
        val originalCheckpoint = lowerBoundHeight + MAX_REORG_SIZE + 2
        return if (height < originalCheckpoint) {
            originalCheckpoint
        } else {
            // tricky: subtract one because we delete ABOVE this block
            // This could create an invalid height if if height was saplingActivationHeight
            val rewindHeight = BlockHeight(height.value - 1)
            rustBackend.getNearestRewindHeight(rewindHeight)
        }
    }

    /**
     * Rewind back at least two weeks worth of blocks.
     */
    suspend fun quickRewind() {
        val height = max(_processorInfo.value.lastSyncedHeight, repository.lastScannedHeight())
        val blocksPer14Days = 14.days.inWholeMilliseconds / ZcashSdk.BLOCK_INTERVAL_MILLIS.toInt()
        val twoWeeksBack = BlockHeight.new(
            network,
            (height.value - blocksPer14Days).coerceAtLeast(lowerBoundHeight.value)
        )
        rewindToNearestHeight(twoWeeksBack, false)
    }

    /**
     * @param alsoClearBlockCache when true, also clear the block cache which forces a redownload of
     * blocks. Otherwise, the cached blocks will be used in the rescan, which in most cases, is fine.
     */
    @Suppress("LongMethod")
    suspend fun rewindToNearestHeight(
        height: BlockHeight,
        alsoClearBlockCache: Boolean = false
    ) {
        processingMutex.withLockLogged("rewindToHeight") {
            val lastSyncedHeight = _processorInfo.value.lastSyncedHeight
            val lastLocalBlock = repository.lastScannedHeight()
            val targetHeight = getNearestRewindHeight(height)

            Twig.debug {
                "Rewinding from $lastSyncedHeight to requested height: $height using target height: " +
                    "$targetHeight with last local block: $lastLocalBlock"
            }

            if (null == lastSyncedHeight && targetHeight < lastLocalBlock) {
                Twig.debug { "Rewinding because targetHeight is less than lastLocalBlock." }
                rustBackend.rewindToHeight(targetHeight)
            } else if (null != lastSyncedHeight && targetHeight < lastSyncedHeight) {
                Twig.debug { "Rewinding because targetHeight is less than lastSyncedHeight." }
                rustBackend.rewindToHeight(targetHeight)
            } else {
                Twig.debug {
                    "Not rewinding dataDb because the last synced height is $lastSyncedHeight and the" +
                        " last local block is $lastLocalBlock both of which are less than the target height of " +
                        "$targetHeight"
                }
            }

            val currentNetworkBlockHeight = _processorInfo.value.networkBlockHeight

            if (alsoClearBlockCache) {
                Twig.debug {
                    "Also clearing block cache back to $targetHeight. These rewound blocks will download " +
                        "in the next scheduled scan"
                }
                downloader.rewindToHeight(targetHeight)
                // communicate that the wallet is no longer synced because it might remain this way for 20+ second
                // because we only download on 20s time boundaries so we can't trigger any immediate action
                setState(State.Syncing)
                if (null == currentNetworkBlockHeight) {
                    updateProgress(
                        lastSyncedHeight = targetHeight,
                        lastSyncRange = null
                    )
                } else {
                    updateProgress(
                        lastSyncedHeight = targetHeight,
                        lastSyncRange = (targetHeight + 1)..currentNetworkBlockHeight
                    )
                }
                _progress.value = PercentDecimal.ZERO_PERCENT
            } else {
                if (null == currentNetworkBlockHeight) {
                    updateProgress(
                        lastSyncedHeight = targetHeight,
                        lastSyncRange = null
                    )
                } else {
                    updateProgress(
                        lastSyncedHeight = targetHeight,
                        lastSyncRange = (targetHeight + 1)..currentNetworkBlockHeight
                    )
                }

                _progress.value = PercentDecimal.ZERO_PERCENT

                if (null != lastSyncedHeight) {
                    val range = (targetHeight + 1)..lastSyncedHeight
                    Twig.debug {
                        "We kept the cache blocks in place so we don't need to wait for the next " +
                            "scheduled download to rescan. Instead we will rescan and validate blocks " +
                            "${range.start}..${range.endInclusive}"
                    }

                    syncBlocksAndEnhanceTransactions(
                        syncRange = range,
                        withDownload = false
                    )
                }
            }
        }
    }

    /** insightful function for debugging these critical errors */
    private suspend fun printValidationErrorInfo(errorHeight: BlockHeight, count: Int = 11) {
        // Note: blocks are public information so it's okay to print them but, still, let's not unless we're
        // debugging something
        if (!BuildConfig.DEBUG) {
            return
        }

        var errorInfo = fetchValidationErrorInfo(errorHeight)
        Twig.debug { "validation failed at block ${errorInfo.errorHeight} with hash: ${errorInfo.hash}" }

        errorInfo = fetchValidationErrorInfo(errorHeight + 1)
        Twig.debug { "the next block is ${errorInfo.errorHeight} with hash: ${errorInfo.hash}" }

        Twig.debug { "=================== BLOCKS [$errorHeight..${errorHeight.value + count - 1}]: START ========" }
        repeat(count) { i ->
            val height = errorHeight + i
            val block = downloader.compactBlockRepository.findCompactBlock(height)
            // sometimes the initial block was inserted via checkpoint and will not appear in the cache. We can get
            // the hash another way.
            val checkedHash = block?.hash ?: repository.findBlockHash(height)
            Twig.debug { "block: $height\thash=${checkedHash?.toHexReversed()}" }
        }
        Twig.debug { "=================== BLOCKS [$errorHeight..${errorHeight.value + count - 1}]: END ========" }
    }

    private suspend fun fetchValidationErrorInfo(errorHeight: BlockHeight): ValidationErrorInfo {
        val hash = repository.findBlockHash(errorHeight + 1)?.toHexReversed()

        return ValidationErrorInfo(errorHeight, hash)
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

    private fun determineLowerBound(errorHeight: BlockHeight): BlockHeight {
        val offset = min(MAX_REORG_SIZE, REWIND_DISTANCE * (consecutiveChainErrors.get() + 1))
        return BlockHeight(max(errorHeight.value - offset, lowerBoundHeight.value)).also {
            Twig.debug {
                "offset = min($MAX_REORG_SIZE, $REWIND_DISTANCE * (${consecutiveChainErrors.get() + 1})) = " +
                    "$offset"
            }
            Twig.debug { "lowerBound = max($errorHeight - $offset, $lowerBoundHeight) = $it" }
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
    @Suppress("UNUSED_PARAMETER")
    private fun calculatePollInterval(fastIntervalDesired: Boolean = false): Long {
        val interval = POLL_INTERVAL
        val now = System.currentTimeMillis()
        val deltaToNextInteral = interval - (now + interval).rem(interval)
        return deltaToNextInteral
    }

    suspend fun calculateBirthdayHeight(): BlockHeight {
        var oldestTransactionHeight: BlockHeight? = null
        @Suppress("TooGenericExceptionCaught")
        try {
            val tempOldestTransactionHeight = repository.getOldestTransaction()?.minedHeight
                ?: lowerBoundHeight
            // to be safe adjust for reorgs (and generally a little cushion is good for privacy)
            // so we round down to the nearest 100 and then subtract 100 to ensure that the result is always at least
            // 100 blocks away
            oldestTransactionHeight = BlockHeight.new(
                network,
                tempOldestTransactionHeight.value -
                    tempOldestTransactionHeight.value.rem(MAX_REORG_SIZE) - MAX_REORG_SIZE.toLong()
            )
        } catch (t: Throwable) {
            Twig.debug(t) { "failed to calculate birthday" }
        }
        return buildList<BlockHeight> {
            add(lowerBoundHeight)
            add(rustBackend.network.saplingActivationHeight)
            oldestTransactionHeight?.let { add(it) }
        }.maxOf { it }
    }

    /**
     * Calculates the latest balance info.
     *
     * @param account the account to check for balance info.
     *
     * @return an instance of WalletBalance containing information about available and total funds.
     */
    suspend fun getBalanceInfo(account: Account): WalletBalance {
        @Suppress("TooGenericExceptionCaught")
        return try {
            val balanceTotal = rustBackend.getBalance(account)
            Twig.debug { "found total balance: $balanceTotal" }
            val balanceAvailable = rustBackend.getVerifiedBalance(account)
            Twig.debug { "found available balance: $balanceAvailable" }
            WalletBalance(balanceTotal, balanceAvailable)
        } catch (t: Throwable) {
            Twig.debug { "failed to get balance due to $t" }
            throw RustLayerException.BalanceException(t)
        }
    }

    suspend fun getUtxoCacheBalance(address: String): WalletBalance =
        rustBackend.getDownloadedUtxoBalance(address)

    /**
     * Transmits the given state for this processor.
     */
    private suspend fun setState(newState: State) {
        _state.value = newState
    }

    /**
     * Sealed class representing the various states of this processor.
     */
    sealed class State {
        /**
         * Marker interface for [State] instances that represent when the wallet is connected.
         */
        interface IConnected

        /**
         * Marker interface for [State] instances that represent when the wallet is syncing.
         */
        interface ISyncing

        /**
         * [State] for common syncing stage. It starts with downloading new blocks, then validating these blocks
         * and scanning them at the end.
         *
         * **Downloading** is when the wallet is actively downloading compact blocks because the latest
         * block height available from the server is greater than what we have locally. We move out
         * of this state once our local height matches the server.
         *
         * **Validating** is when the blocks that have been downloaded are actively being validated to
         * ensure that there are no gaps and that every block is chain-sequential to the previous
         * block, which determines whether a reorg has happened on our watch.
         *
         * **Scanning** is when the blocks that have been downloaded are actively being decrypted.
         */
        object Syncing : IConnected, ISyncing, State()

        /**
         * [State] for when we are done with syncing the blocks, for now, i.e. all necessary stages done (download,
         * validate, and scan).
         */
        class Synced(val syncedRange: ClosedRange<BlockHeight>?) : IConnected, ISyncing, State()

        /**
         * [State] for when transaction details are being retrieved. This typically means the wallet
         * has downloaded and scanned blocks and is now processing any transactions that were
         * discovered. Once a transaction is discovered, followup network requests are needed in
         * order to retrieve memos or outbound transaction information, like the recipient address.
         * The existing information we have about transactions is enhanced by the new information.
         */
        object Enhancing : IConnected, ISyncing, State()

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
     * Progress model class for sharing the whole batch sync progress out of the sync process.
     */
    internal data class BatchSyncProgress(
        val percentage: PercentDecimal,
        val lastSyncedHeight: BlockHeight?,
        val result: BlockProcessingResult
    )

    /**
     * Progress model class for sharing particular sync stage result internally in the sync process.
     */
    private data class SyncStageResult(
        val batch: BlockBatch,
        val stageResult: BlockProcessingResult
    )

    /**
     * Data class for holding detailed information about the processor.
     *
     * @param networkBlockHeight the latest block available to lightwalletd that may or may not be
     * downloaded by this wallet yet.
     * @param lastSyncedHeight the height up to which the wallet last synced. This determines
     * where the next sync will begin.
     * @param lastSyncRange inclusive range to sync. Meaning, if the range is 10..10,
     * then we will download exactly block 10. If the range is 11..10, then we want to download
     * block 11 but can't.
     */
    data class ProcessorInfo(
        val networkBlockHeight: BlockHeight?,
        val lastSyncedHeight: BlockHeight?,
        val lastSyncRange: ClosedRange<BlockHeight>?
    ) {

        /**
         * Determines whether this instance has data.
         *
         * @return false when all values match their defaults.
         */
        val hasData
            get() = networkBlockHeight != null ||
                lastSyncedHeight != null ||
                lastSyncRange != null

        /**
         * Determines whether this instance is actively syncing compact blocks.
         *
         * @return true when there are more than zero blocks remaining to sync.
         */
        val isSyncing: Boolean
            get() =
                lastSyncedHeight != null &&
                    lastSyncRange != null &&
                    !lastSyncRange.isEmpty() &&
                    lastSyncedHeight < lastSyncRange.endInclusive

        /**
         * The amount of sync progress from 0 to 100.
         */
        @Suppress("MagicNumber")
        val syncProgress
            get() = when {
                lastSyncedHeight == null -> 0
                lastSyncRange == null -> 100
                lastSyncedHeight >= lastSyncRange.endInclusive -> 100
                else -> {
                    // when lastSyncedHeight == lastSyncedRange.first, we have synced one block, thus the offsets
                    val blocksSynced =
                        (lastSyncedHeight.value - lastSyncRange.start.value + 1).coerceAtLeast(0)
                    // we sync the range inclusively so 100..100 is one block to sync, thus the offset
                    val numberOfBlocks =
                        lastSyncRange.endInclusive.value - lastSyncRange.start.value + 1
                    // take the percentage then convert and round
                    ((blocksSynced.toFloat() / numberOfBlocks) * 100.0f).coerceAtMost(100.0f).roundToInt()
                }
            }
    }

    data class ValidationErrorInfo(
        val errorHeight: BlockHeight,
        val hash: String?
    )

    //
    // Helper Extensions
    //

    /**
     * Log the mutex in great detail just in case we need it for troubleshooting deadlock.
     */
    private suspend inline fun <T> Mutex.withLockLogged(name: String, block: () -> T): T {
        Twig.debug { "$name MUTEX: acquiring lock..." }
        this.withLock {
            Twig.debug { "$name MUTEX: ...lock acquired!" }
            return block().also {
                Twig.debug { "$name MUTEX: releasing lock" }
            }
        }
    }
}

private fun LightWalletEndpointInfoUnsafe.matchingNetwork(network: String): Boolean {
    fun String.toId() = lowercase(Locale.ROOT).run {
        when {
            contains("main") -> "mainnet"
            contains("test") -> "testnet"
            else -> this
        }
    }
    return chainName.toId() == network.toId()
}

private fun max(a: BlockHeight?, b: BlockHeight) = if (null == a) {
    b
} else if (a.value > b.value) {
    a
} else {
    b
}
