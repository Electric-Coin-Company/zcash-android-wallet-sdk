package cash.z.ecc.android.sdk.block.processor

import androidx.annotation.VisibleForTesting
import cash.z.ecc.android.sdk.BuildConfig
import cash.z.ecc.android.sdk.annotation.OpenForTesting
import cash.z.ecc.android.sdk.block.processor.model.BatchSyncProgress
import cash.z.ecc.android.sdk.block.processor.model.GetMaxScannedHeightResult
import cash.z.ecc.android.sdk.block.processor.model.GetSubtreeRootsResult
import cash.z.ecc.android.sdk.block.processor.model.GetWalletSummaryResult
import cash.z.ecc.android.sdk.block.processor.model.PutSaplingSubtreeRootsResult
import cash.z.ecc.android.sdk.block.processor.model.SbSPreparationResult
import cash.z.ecc.android.sdk.block.processor.model.SuggestScanRangesResult
import cash.z.ecc.android.sdk.block.processor.model.SyncStageResult
import cash.z.ecc.android.sdk.block.processor.model.SyncingResult
import cash.z.ecc.android.sdk.block.processor.model.UpdateChainTipResult
import cash.z.ecc.android.sdk.block.processor.model.VerifySuggestedScanRange
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException.EnhanceTransactionError.EnhanceTxDecryptError
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException.EnhanceTransactionError.EnhanceTxDownloadError
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException.MismatchedNetwork
import cash.z.ecc.android.sdk.exception.InitializeException
import cash.z.ecc.android.sdk.exception.LightWalletException
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.ZcashSdk.MAX_BACKOFF_INTERVAL
import cash.z.ecc.android.sdk.ext.ZcashSdk.POLL_INTERVAL
import cash.z.ecc.android.sdk.ext.ZcashSdk.POLL_INTERVAL_SHORT
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.TypesafeBackend
import cash.z.ecc.android.sdk.internal.block.CompactBlockDownloader
import cash.z.ecc.android.sdk.internal.ext.isNullOrEmpty
import cash.z.ecc.android.sdk.internal.ext.isScanContinuityError
import cash.z.ecc.android.sdk.internal.ext.length
import cash.z.ecc.android.sdk.internal.ext.retryUpToAndContinue
import cash.z.ecc.android.sdk.internal.ext.retryUpToAndThrow
import cash.z.ecc.android.sdk.internal.ext.retryWithBackoff
import cash.z.ecc.android.sdk.internal.ext.toHexReversed
import cash.z.ecc.android.sdk.internal.metrics.TraceScope
import cash.z.ecc.android.sdk.internal.model.BlockBatch
import cash.z.ecc.android.sdk.internal.model.DbTransactionOverview
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.model.ScanRange
import cash.z.ecc.android.sdk.internal.model.SubtreeRoot
import cash.z.ecc.android.sdk.internal.model.SuggestScanRangePriority
import cash.z.ecc.android.sdk.internal.model.WalletSummary
import cash.z.ecc.android.sdk.internal.model.ext.from
import cash.z.ecc.android.sdk.internal.model.ext.toBlockHeight
import cash.z.ecc.android.sdk.internal.repository.DerivedDataRepository
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.PercentDecimal
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.GetAddressUtxosReplyUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.model.ShieldedProtocolEnum
import co.electriccoin.lightwallet.client.model.SubtreeRootUnsafe
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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Responsible for processing the compact blocks that are received from the lightwallet server. This class encapsulates
 * all the business logic required to validate and scan the blockchain and is therefore tightly coupled with
 * librustzcash.
 *
 * @property downloader the component responsible for downloading compact blocks and persisting them
 * locally for processing.
 * @property repository the repository holding transaction information.
 * @property backend the librustzcash functionality available and exposed to the SDK.
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
    private val backend: TypesafeBackend,
    minimumHeight: BlockHeight
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

    private val consecutiveChainErrors = AtomicInteger(0)

    private val consecutiveBlockProcessingErrors = AtomicInteger(0)

    /**
     * The zcash network that is being processed. Either Testnet or Mainnet.
     */
    val network = backend.network

    private val lowerBoundHeight: BlockHeight =
        BlockHeight(
            max(
                network.saplingActivationHeight.value,
                minimumHeight.value - MAX_REORG_SIZE
            )
        )

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Initialized)
    private val _progress = MutableStateFlow(PercentDecimal.ZERO_PERCENT)
    private val _processorInfo = MutableStateFlow(ProcessorInfo(null, null, null))
    private val _networkHeight = MutableStateFlow<BlockHeight?>(null)

    // pools
    internal val saplingBalances = MutableStateFlow<WalletBalance?>(null)
    internal val orchardBalances = MutableStateFlow<WalletBalance?>(null)
    internal val transparentBalances = MutableStateFlow<WalletBalance?>(null)

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
     * The flow of network height. This value is updated at the same time that [processorInfo] is
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
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    suspend fun start() {
        val traceScope = TraceScope("CompactBlockProcessor.start")

        val startIndex = refreshWalletSummary()

        verifySetup()

        updateBirthdayHeight()

        // Clear any undeleted left over block files from previous sync attempts.
        // lastKnownHeight is only used for error reporting.
        deleteAllBlockFiles(
            downloader = downloader,
            lastKnownHeight =
                when (val result = getMaxScannedHeight(backend)) {
                    is GetMaxScannedHeightResult.Success -> result.height
                    else -> null
                }
        )

        // Download note commitment tree data from lightwalletd to decide if we communicate with linear
        // or spend-before-sync node.
        var subTreeRootResult = getSubtreeRoots(downloader, network, startIndex)
        Twig.info { "Fetched SubTreeRoot result: $subTreeRootResult" }

        Twig.debug { "Setup verified. Processor starting..." }

        // Reset all error counters at the beginning of the new block synchronization cycle
        resetErrorCounters()

        // Using do/while makes it easier to execute exactly one loop which helps with testing this processor quickly
        // (because you can start and then immediately set isStopped=true to always get precisely one loop)
        do {
            retryWithBackoff(
                onErrorListener = ::onProcessorError,
                maxDelayMillis = MAX_BACKOFF_INTERVAL
            ) {
                val result =
                    processingMutex.withLockLogged("processNewBlocks") {
                        when (subTreeRootResult) {
                            is GetSubtreeRootsResult.SpendBeforeSync -> {
                                // Pass the commitment tree data to the database
                                val putScope = TraceScope("CompactBlockProcessor.putSaplingSubtreeRoots")
                                when (
                                    val result =
                                        putSaplingSubtreeRoots(
                                            backend = backend,
                                            startIndex = startIndex,
                                            subTreeRootList =
                                                (subTreeRootResult as GetSubtreeRootsResult.SpendBeforeSync)
                                                    .subTreeRootList,
                                            lastValidHeight = lowerBoundHeight
                                        )
                                ) {
                                    PutSaplingSubtreeRootsResult.Success -> {
                                        // Let's continue with the next step
                                    }
                                    is PutSaplingSubtreeRootsResult.Failure -> {
                                        BlockProcessingResult.SyncFailure(result.failedAtHeight, result.exception)
                                    }
                                }
                                putScope.end()
                                processNewBlocksInSbSOrder(
                                    backend = backend,
                                    downloader = downloader,
                                    repository = repository,
                                    network = network,
                                    lastValidHeight = lowerBoundHeight,
                                    firstUnenhancedHeight = _processorInfo.value.firstUnenhancedHeight
                                )
                            }
                            is GetSubtreeRootsResult.OtherFailure, GetSubtreeRootsResult.Linear -> {
                                // This is caused by an empty response result or another unsupported error.
                                // Although the spend-before-sync synchronization algorithm is not supported, we can get
                                // the entire block range as we previously did for the linear sync type.
                                processNewBlocksInSbSOrder(
                                    backend = backend,
                                    downloader = downloader,
                                    repository = repository,
                                    network = network,
                                    lastValidHeight = lowerBoundHeight,
                                    firstUnenhancedHeight = _processorInfo.value.firstUnenhancedHeight
                                )
                            }
                            GetSubtreeRootsResult.FailureConnection -> {
                                // SubtreeRoot fetching retry
                                subTreeRootResult = getSubtreeRoots(downloader, network, startIndex)
                                BlockProcessingResult.Reconnecting
                            }
                        }
                    }

                // Immediately process again after failures in order to download new blocks right away
                when (result) {
                    BlockProcessingResult.Reconnecting -> {
                        setState(State.Disconnected)
                        downloader.reconnect()
                        takeANap(
                            "Unable to process new blocks because we are disconnected! Attempting to reconnect.",
                            true
                        )
                    }
                    BlockProcessingResult.RestartSynchronization -> {
                        Twig.info { "Planned restarting of block synchronization..." }

                        resetErrorCounters()

                        // No nap time set to immediately continue with refreshed block synchronization
                    }
                    BlockProcessingResult.NoBlocksToProcess -> {
                        setState(State.Synced(_processorInfo.value.overallSyncRange))
                        val noWorkDone = _processorInfo.value.overallSyncRange?.isEmpty() ?: true
                        val summary =
                            if (noWorkDone) {
                                "Nothing to process: no new blocks to sync"
                            } else {
                                "Done processing blocks"
                            }

                        resetErrorCounters()
                        takeANap(summary, false)
                    }
                    is BlockProcessingResult.ContinuityError -> {
                        Twig.error {
                            "Failed while processing blocks at height: ${result.failedAtHeight} with continuity " +
                                "error: ${result.error}"
                        }
                        handleChainError(result.failedAtHeight)
                        // No nap time set to immediately continue with the following block synchronization attempt
                    }
                    is BlockProcessingResult.SyncFailure -> {
                        Twig.error {
                            "Failed while processing blocks at height: ${result.failedAtHeight} with: " +
                                "${result.error}"
                        }
                        val failed = checkErrorAndFail(result.failedAtHeight, result.error)
                        if (!failed) {
                            takeANap("", true)
                        }
                    }
                    is BlockProcessingResult.Success -> {
                        resetErrorCounters()
                    }
                }
            }
        } while (_state.value !is State.Stopped)
        traceScope.end()
        Twig.info { "Processor complete" }
        stop()
    }

    private fun resetErrorCounters() {
        consecutiveBlockProcessingErrors.set(0)
        consecutiveChainErrors.set(0)
    }

    /**
     * Checks the block synchronization retry attempts and fails with throwing the [failCause] error once [RETRIES]
     * count is reached.
     *
     * @param failedHeight The height at which the block processing failed
     * @param failCause The cause of the failure to be part of thrown exception
     *
     * @return `True` when the retry limit reached and the error thrown, false when error counter increment and no
     * error thrown yet
     */
    suspend fun checkErrorAndFail(
        failedHeight: BlockHeight?,
        failCause: Throwable
    ): Boolean {
        if (consecutiveBlockProcessingErrors.get() >= RETRIES) {
            val errorMessage =
                "ERROR: unable to resolve the error at height $failedHeight after " +
                    "${consecutiveBlockProcessingErrors.get()} correction attempts!"
            fail(CompactBlockProcessorException.FailedSynchronizationException(errorMessage, failCause))
            return true
        }
        consecutiveBlockProcessingErrors.getAndIncrement()
        return false
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
        Twig.error { "${error.message}" }
        throw error
    }

    // TODO [#1137]: Refactor processNewBlocksInSbSOrder
    // TODO [#1137]: https://github.com/zcash/zcash-android-wallet-sdk/issues/1137

    /**
     * This function process the missing blocks in non-linear order with Spend-before-Sync algorithm.
     */
    @Suppress("ReturnCount", "LongMethod", "CyclomaticComplexMethod", "LongParameterList")
    private suspend fun processNewBlocksInSbSOrder(
        backend: TypesafeBackend,
        downloader: CompactBlockDownloader,
        repository: DerivedDataRepository,
        network: ZcashNetwork,
        lastValidHeight: BlockHeight,
        firstUnenhancedHeight: BlockHeight?
    ): BlockProcessingResult {
        Twig.info {
            "Beginning to process new blocks with Spend-before-Sync approach with lower bound: $lastValidHeight)..."
        }
        val traceScope = TraceScope("CompactBlockProcessor.processNewBlocksInSbSOrder")

        // This step covers these operations fetchLatestBlockHeight, updateChainTip, suggestScanRanges, updateRange,
        // and shouldVerifySuggestedScanRanges
        val preparationResult =
            runSbSSyncingPreparation(
                backend = backend,
                downloader = downloader,
                network = network,
                lastValidHeight = lastValidHeight
            )
        when (preparationResult) {
            is SbSPreparationResult.ProcessFailure -> {
                return preparationResult.toBlockProcessingResult()
            }
            SbSPreparationResult.ConnectionFailure -> {
                return BlockProcessingResult.Reconnecting
            }
            SbSPreparationResult.NoMoreBlocksToProcess -> {
                return BlockProcessingResult.NoBlocksToProcess
            }
            is SbSPreparationResult.Success -> {
                Twig.info { "Preparation phase done with result: $preparationResult" }
                // Continue processing ranges
            }
        }

        var verifyRangeResult = preparationResult.verifyRangeResult
        var suggestedRangesResult = preparationResult.suggestedRangesResult
        val lastPreparationTime = System.currentTimeMillis()

        // Running synchronization for the [ScanRange.SuggestScanRangePriority.Verify] range
        while (verifyRangeResult is VerifySuggestedScanRange.ShouldVerify) {
            Twig.info { "Starting verification of range: $verifyRangeResult" }

            var syncingResult: SyncingResult = SyncingResult.AllSuccess
            runSyncingAndEnhancingOnRange(
                backend = backend,
                downloader = downloader,
                repository = repository,
                network = network,
                syncRange = verifyRangeResult.scanRange.range,
                withDownload = true,
                enhanceStartHeight = firstUnenhancedHeight
            ).collect { batchSyncProgress ->
                // Update sync progress and wallet balance
                refreshWalletSummary()

                when (batchSyncProgress.resultState) {
                    SyncingResult.UpdateBirthday -> {
                        updateBirthdayHeight()
                    }
                    SyncingResult.EnhanceSuccess -> {
                        Twig.info { "Triggering transaction refresh now" }
                        // Invalidate transaction data
                        checkTransactions(transactionStorage = repository)
                        batchSyncProgress.range?.start?.let {
                            refreshUtxos(account = Account.DEFAULT, startHeight = it)
                        }
                    }
                    is SyncingResult.Failure -> {
                        syncingResult = batchSyncProgress.resultState
                        return@collect
                    } else -> {
                        // Continue with processing
                    }
                }
            }

            when (syncingResult) {
                is SyncingResult.AllSuccess -> {
                    // Continue with processing the rest of the ranges
                }
                is SyncingResult.ContinuityError -> {
                    val failedHeight = (syncingResult as SyncingResult.ContinuityError).failedAtHeight
                    Twig.warn {
                        "Continuity error occurred at height: $failedHeight. Starting to resolve it with " +
                            "rewind action."
                    }
                    // This step is independent of the rewind action as it only removes temporary persisted block files
                    // from the device storage. The files will be re-downloaded in the following synchronization cycle.
                    deleteAllBlockFiles(
                        downloader = downloader,
                        lastKnownHeight = failedHeight
                    )
                    return (syncingResult as SyncingResult.ContinuityError).toBlockProcessingResult()
                }
                is SyncingResult.Failure -> {
                    // Other failure types
                    return (syncingResult as SyncingResult.Failure).toBlockProcessingResult()
                }
                else -> {
                    // The rest types of result are not expected here
                    Twig.info { "Unexpected syncing result: $syncingResult" }
                }
            }

            // Re-request suggested scan ranges
            suggestedRangesResult = suggestScanRanges(backend, lowerBoundHeight)
            when (suggestedRangesResult) {
                is SuggestScanRangesResult.Success -> {
                    verifyRangeResult = shouldVerifySuggestedScanRanges(suggestedRangesResult)
                }
                is SuggestScanRangesResult.Failure -> {
                    Twig.error { "Process suggested scan ranges failure: ${suggestedRangesResult.exception}" }
                    return BlockProcessingResult.SyncFailure(
                        suggestedRangesResult.failedAtHeight,
                        suggestedRangesResult.exception
                    )
                }
            }
        }

        // Process the rest of ranges
        val scanRanges =
            when (suggestedRangesResult) {
                is SuggestScanRangesResult.Success -> {
                    suggestedRangesResult.ranges
                }
                is SuggestScanRangesResult.Failure -> {
                    Twig.error { "Process suggested scan ranges failure: ${suggestedRangesResult.exception}" }
                    return BlockProcessingResult.SyncFailure(
                        suggestedRangesResult.failedAtHeight,
                        suggestedRangesResult.exception
                    )
                }
            }
        scanRanges.forEach { scanRange ->
            Twig.debug { "Start processing the range: $scanRange" }

            // TODO [#1145]: Sync Historic range in reverse order
            // TODO [#1145]: https://github.com/zcash/zcash-android-wallet-sdk/issues/1145
            var syncingResult: SyncingResult = SyncingResult.AllSuccess
            runSyncingAndEnhancingOnRange(
                backend = backend,
                downloader = downloader,
                repository = repository,
                network = network,
                syncRange = scanRange.range,
                withDownload = true,
                enhanceStartHeight = firstUnenhancedHeight
            ).map { batchSyncProgress ->
                // Update sync progress and wallet balance
                refreshWalletSummary()

                when (batchSyncProgress.resultState) {
                    SyncingResult.UpdateBirthday -> {
                        updateBirthdayHeight()
                        SyncingResult.AllSuccess
                    }
                    SyncingResult.EnhanceSuccess -> {
                        Twig.info { "Triggering transaction refresh now" }
                        // Invalidate transaction data and return the common batch syncing success result to the caller
                        checkTransactions(transactionStorage = repository)
                        batchSyncProgress.range?.start?.let {
                            refreshUtxos(account = Account.DEFAULT, startHeight = it)
                        }
                        SyncingResult.AllSuccess
                    }
                    is SyncingResult.Failure -> {
                        batchSyncProgress.resultState
                    } else -> {
                        // First, check the time and refresh the prepare phase inputs, if needed
                        val currentTimeMillis = System.currentTimeMillis()
                        if (shouldRefreshPreparation(
                                lastPreparationTime,
                                currentTimeMillis,
                                SYNCHRONIZATION_RESTART_TIMEOUT
                            )
                        ) {
                            SyncingResult.RestartSynchronization
                        } else {
                            // Continue with processing
                            SyncingResult.AllSuccess
                        }
                    }
                }
            }.takeWhile {
                syncingResult = it
                it == SyncingResult.AllSuccess
            }.collect()

            when (syncingResult) {
                is SyncingResult.AllSuccess -> {
                    // Continue with processing the rest of the ranges
                }
                is SyncingResult.RestartSynchronization -> {
                    // Restarting the synchronization process
                    return BlockProcessingResult.RestartSynchronization
                }
                is SyncingResult.ContinuityError -> {
                    val failedHeight = (syncingResult as SyncingResult.ContinuityError).failedAtHeight
                    Twig.warn {
                        "Continuity error occurred at height: $failedHeight. Starting to resolve it with " +
                            "rewind action."
                    }
                    // This step is independent of the rewind action as it only removes temporary persisted block files
                    // from the device storage. The files will be re-downloaded in the following synchronization cycle.
                    deleteAllBlockFiles(
                        downloader = downloader,
                        lastKnownHeight = failedHeight
                    )
                    return (syncingResult as SyncingResult.ContinuityError).toBlockProcessingResult()
                }
                is SyncingResult.Failure -> {
                    // Other failure types
                    return (syncingResult as SyncingResult.Failure).toBlockProcessingResult()
                }
                else -> {
                    // The rest types of result are not expected here
                    Twig.info { "Unexpected syncing result: $syncingResult" }
                }
            }
        }
        traceScope.end()
        return BlockProcessingResult.Success
    }

    @Suppress("ReturnCount")
    internal suspend fun runSbSSyncingPreparation(
        backend: TypesafeBackend,
        downloader: CompactBlockDownloader,
        network: ZcashNetwork,
        lastValidHeight: BlockHeight
    ): SbSPreparationResult {
        // Download chain tip metadata from lightwalletd
        val chainTip =
            fetchLatestBlockHeight(
                downloader = downloader,
                network = network
            ) ?: let {
                Twig.warn { "Disconnection detected. Attempting to reconnect." }
                return SbSPreparationResult.ConnectionFailure
            }

        // Notify the underlying rust layer about the updated chain tip
        when (
            val result =
                updateChainTip(
                    backend = backend,
                    chainTip = chainTip,
                    lastValidHeight = lastValidHeight
                )
        ) {
            is UpdateChainTipResult.Success -> { /* Let's continue to the next step */ }
            is UpdateChainTipResult.Failure -> {
                return SbSPreparationResult.ProcessFailure(
                    result.failedAtHeight,
                    result.exception
                )
            }
        }

        // TODO [#1211]: Re-enable block synchronization benchmark test
        // TODO [#1211]: https://github.com/zcash/zcash-android-wallet-sdk/issues/1211

        // Get the suggested scan ranges from the wallet database
        val suggestedRangesResult =
            suggestScanRanges(
                backend,
                lastValidHeight
            )
        val updateRangeResult =
            when (suggestedRangesResult) {
                is SuggestScanRangesResult.Success -> {
                    updateRange(suggestedRangesResult.ranges)
                }
                is SuggestScanRangesResult.Failure -> {
                    Twig.error { "Process suggested scan ranges failure: ${suggestedRangesResult.exception}" }
                    return SbSPreparationResult.ProcessFailure(
                        suggestedRangesResult.failedAtHeight,
                        suggestedRangesResult.exception
                    )
                }
            }

        if (!updateRangeResult) {
            Twig.warn { "Disconnection detected. Attempting to reconnect." }
            return SbSPreparationResult.ConnectionFailure
        } else if (_processorInfo.value.overallSyncRange.isNullOrEmpty()) {
            Twig.info { "No more blocks to process." }
            return SbSPreparationResult.NoMoreBlocksToProcess
        }

        setState(State.Syncing)

        // Parse and process ranges. If it recognizes a range with Priority.Verify, it runs the verification part.
        val verifyRangeResult = shouldVerifySuggestedScanRanges(suggestedRangesResult)

        Twig.info { "Check for verification of ranges resulted with: $verifyRangeResult" }

        return SbSPreparationResult.Success(
            suggestedRangesResult = suggestedRangesResult,
            verifyRangeResult = verifyRangeResult
        )
    }

    /**
     * This invalidates transaction storage to trigger data refreshing for its subscribers.
     */
    private fun checkTransactions(transactionStorage: DerivedDataRepository) {
        transactionStorage.invalidate()
    }

    /**
     * Update the latest balances using the given wallet summary, and transmit this information
     * into the related internal flows. Note that the Orchard balance is not supported.
     */
    internal suspend fun updateAllBalances(summary: WalletSummary) {
        summary.accountBalances[Account.DEFAULT]?.let {
            Twig.debug { "Updating Sapling balance" }
            saplingBalances.value = it.sapling
            // TODO [#682]: Uncomment this once we have Orchard support.
            // TODO [#682]: https://github.com/zcash/zcash-android-wallet-sdk/issues/682
            // orchardBalances.value = it.orchard
            // We only allow stored transparent balance to be shielded, and we do so with
            // a zero-conf transaction, so treat all unshielded balance as available.
            transparentBalances.value =
                WalletBalance(
                    it.unshielded,
                    it.unshielded
                )
        }
    }

    /**
     * Refreshes the SDK's wallet summary from the Rust backend, and transmits this information
     * into the related internal flows. Note that the Orchard balance is not yet supported.
     *
     * @return the next subtree index to fetch.
     */
    internal suspend fun refreshWalletSummary(): UInt {
        when (val result = getWalletSummary(backend)) {
            is GetWalletSummaryResult.Success -> {
                val resultProgress = result.scanProgressPercentDecimal()
                Twig.info { "Progress from rust: ${resultProgress.decimal}" }
                setProgress(resultProgress)
                updateAllBalances(result.walletSummary)
                return result.walletSummary.nextSaplingSubtreeIndex
            }
            else -> {
                // Do not report the progress and balances in case of any error, and
                // tell the caller to fetch all subtree roots.
                return UInt.MIN_VALUE
            }
        }
    }

    sealed class BlockProcessingResult {
        object NoBlocksToProcess : BlockProcessingResult()

        object Success : BlockProcessingResult()

        object Reconnecting : BlockProcessingResult()

        object RestartSynchronization : BlockProcessingResult()

        data class SyncFailure(val failedAtHeight: BlockHeight?, val error: Throwable) : BlockProcessingResult()

        data class ContinuityError(val failedAtHeight: BlockHeight?, val error: Throwable) : BlockProcessingResult()
    }

    /**
     * Gets the latest range info and then uses that initialInfo to update (and transmit)
     * the info that require processing.
     *
     * @param ranges The ranges which we obtained from the rust layer to proceed
     *
     * @return true when the update succeeds.
     */
    private suspend fun updateRange(ranges: List<ScanRange>): Boolean {
        // This fetches the latest height each time this method is called, which can be very inefficient
        // when downloading all of the blocks from the server
        val networkBlockHeight = fetchLatestBlockHeight(downloader, network) ?: return false

        // Get the first un-enhanced transaction from the repository
        val firstUnenhancedHeight = getFirstUnenhancedHeight(repository)

        // The overall sync range computation
        val syncRange =
            if (ranges.isNotEmpty()) {
                var resultRange = ranges[0].range.start..ranges[0].range.endInclusive
                ranges.forEach { nextRange ->
                    if (nextRange.range.start < resultRange.start) {
                        resultRange = nextRange.range.start..resultRange.endInclusive
                    }
                    if (nextRange.range.endInclusive > resultRange.endInclusive) {
                        resultRange = resultRange.start..nextRange.range.endInclusive
                    }
                }
                resultRange
            } else {
                // Empty ranges most likely means that the sync is done and the Rust layer replied with an empty
                // suggested ranges
                null
            }

        setProcessorInfo(
            networkBlockHeight = networkBlockHeight,
            overallSyncRange = syncRange,
            firstUnenhancedHeight = firstUnenhancedHeight
        )

        return true
    }

    // TODO [#1127]: Refactor CompactBlockProcessor.verifySetup
    // TODO [#1127]: Need to refactor this to be less ugly and more testable
    // TODO [#1127]: https://github.com/zcash/zcash-android-wallet-sdk/issues/1127

    /**
     * Confirm that the wallet data is properly setup for use.
     */
    @Suppress("NestedBlockDepth")
    private suspend fun verifySetup() {
        val error =
            if (repository.getAccountCount() == 0) {
                CompactBlockProcessorException.NoAccount
            } else {
                // verify that the server is correct

                // How do we handle network connection issues?

                downloader.getServerInfo()?.let { info ->
                    val serverBlockHeight =
                        runCatching { info.blockHeightUnsafe.toBlockHeight(network) }.getOrNull()

                    if (null == serverBlockHeight) {
                        // Note: we could better signal network connection issue
                        CompactBlockProcessorException.BadBlockHeight(info.blockHeightUnsafe)
                    } else {
                        val clientBranch =
                            "%x".format(
                                Locale.ROOT,
                                backend.getBranchIdForHeight(serverBlockHeight)
                            )
                        val network = backend.network.networkName

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
        val betterBirthday = calculateBirthdayHeight()
        if (betterBirthday > birthdayHeight) {
            Twig.debug { "Better birthday found! Birthday height updated from $birthdayHeight to $betterBirthday" }
            _birthdayHeight.value = betterBirthday
        }
    }

    var failedUtxoFetches = 0

    @Suppress("MagicNumber", "LongMethod")
    internal suspend fun refreshUtxos(
        account: Account,
        startHeight: BlockHeight
    ): Int {
        Twig.debug { "Checking for UTXOs above height $startHeight" }
        var count = 0
        // TODO [#683]: cleanup the way that we prevent this from running excessively
        //       For now, try for about 3 blocks per app launch. If the service fails it is
        //       probably disabled on ligthtwalletd, so then stop trying until the next app launch.
        // TODO [#683]: https://github.com/zcash/zcash-android-wallet-sdk/issues/683
        if (failedUtxoFetches < 9) { // there are 3 attempts per block
            @Suppress("TooGenericExceptionCaught")
            try {
                retryUpToAndThrow(UTXO_FETCH_RETRIES) {
                    val tAddresses = backend.listTransparentReceivers(account)

                    downloader.fetchUtxos(
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
                            Twig.verbose { "Fetched UTXO at height: ${utxo.height}" }
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
                "Warning: gave up on fetching UTXOs for this session. It is unavailable on lightwalletd."
            }
        }

        return count
    }

    /**
     * @return True in case of the UTXO processed successfully, false otherwise
     */
    internal suspend fun processUtxoResult(utxo: GetAddressUtxosReplyUnsafe): Boolean {
        // Note (str4d): We no longer clear UTXOs here, as rustBackend.putUtxo now uses an upsert instead of an insert.
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
            backend.putUtxo(
                utxo.address,
                utxo.txid,
                utxo.index,
                utxo.script,
                utxo.valueZat,
                BlockHeight.new(backend.network, utxo.height)
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
         * Transaction fetching default attempts at retrying.
         */
        internal const val TRANSACTION_FETCH_RETRIES = 1

        /**
         * UTXOs fetching default attempts at retrying.
         */
        internal const val UTXO_FETCH_RETRIES = 3

        /**
         * Latest block height fetching default attempts at retrying.
         */
        internal const val FETCH_LATEST_BLOCK_HEIGHT_RETRIES = 3

        /**
         * Get subtree roots default attempts at retrying.
         */
        internal const val GET_SUBTREE_ROOTS_RETRIES = 3

        /**
         * The theoretical maximum number of blocks in a reorg, due to other bottlenecks in the protocol design.
         */
        internal const val MAX_REORG_SIZE = 100

        /**
         * Default size of batches of blocks to request from the compact block service. Then it's also used as a default
         * size of batches of blocks to scan via librustzcash. The smaller the batch size for scanning, the more
         * granular information can be provided about scan state. Unfortunately, small batches may also lead to a lot
         * of overhead during scanning.
         */
        internal const val SYNC_BATCH_SIZE = 100

        /**
         * Default size of batch of blocks for running the transaction enhancing.
         */
        internal const val ENHANCE_BATCH_SIZE = 1000

        /**
         * Default number of blocks to rewind when a chain reorg is detected. This should be large enough to recover
         * from the reorg but smaller than the theoretical max reorg size of 100.
         */
        internal const val REWIND_DISTANCE = 10

        /**
         * Timeout in milliseconds for restarting the currently running block synchronization.
         */
        internal val SYNCHRONIZATION_RESTART_TIMEOUT = 10.minutes.inWholeMilliseconds

        /**
         * Check for the next restart of the block synchronization preparation phase. This function is only SbS
         * synchronization algorithm-related.
         */
        internal fun shouldRefreshPreparation(
            lastPreparationTime: Long,
            currentTimeMillis: Long,
            limitTime: Long
        ): Boolean {
            return (currentTimeMillis - lastPreparationTime) >= limitTime
        }

        /**
         * This operation fetches and returns the latest block height (chain tip)
         *
         * @return Latest block height wrapped in BlockHeight object, or null in case of failure
         */
        @VisibleForTesting
        internal suspend fun fetchLatestBlockHeight(
            downloader: CompactBlockDownloader,
            network: ZcashNetwork
        ): BlockHeight? {
            Twig.debug { "Fetching latest block height..." }
            val traceScope = TraceScope("CompactBlockProcessor.fetchLatestBlockHeight")

            var latestBlockHeight: BlockHeight? = null

            retryUpToAndContinue(FETCH_LATEST_BLOCK_HEIGHT_RETRIES) {
                when (val response = downloader.getLatestBlockHeight()) {
                    is Response.Success -> {
                        Twig.debug { "Latest block height fetched successfully with value: ${response.result.value}" }
                        latestBlockHeight =
                            runCatching {
                                response.result.toBlockHeight(network)
                            }.getOrNull()
                    }
                    is Response.Failure -> {
                        Twig.error { "Fetching latest block height failed with: ${response.toThrowable()}" }
                        traceScope.end()
                        throw LightWalletException.GetLatestBlockHeightException(
                            response.code,
                            response.description,
                            response.toThrowable()
                        )
                    }
                }
            }

            traceScope.end()
            return latestBlockHeight
        }

        /**
         * This operation downloads note commitment tree data from the lightwalletd server, to decide whether or not
         * the lightwalletd and its backing node support spend-before-sync.
         *
         * @return GetSubtreeRootsResult as a wrapper for the lightwalletd response result
         */
        @VisibleForTesting
        internal suspend fun getSubtreeRoots(
            downloader: CompactBlockDownloader,
            network: ZcashNetwork,
            startIndex: UInt
        ): GetSubtreeRootsResult {
            Twig.debug { "Fetching SubtreeRoots..." }
            val traceScope = TraceScope("CompactBlockProcessor.getSubtreeRoots")

            var result: GetSubtreeRootsResult = GetSubtreeRootsResult.Linear

            retryUpToAndContinue(GET_SUBTREE_ROOTS_RETRIES) {
                downloader.getSubtreeRoots(
                    startIndex,
                    maxEntries = UInt.MIN_VALUE,
                    shieldedProtocol = ShieldedProtocolEnum.SAPLING
                ).onEach { response ->
                    when (response) {
                        is Response.Success -> {
                            Twig.verbose {
                                "SubtreeRoot fetched successfully: its completingHeight is: ${response.result
                                    .completingBlockHeight}"
                            }
                        }
                        is Response.Failure -> {
                            val error =
                                LightWalletException.GetSubtreeRootsException(
                                    response.code,
                                    response.description,
                                    response.toThrowable()
                                )
                            if (response is Response.Failure.Server.Unavailable) {
                                Twig.error {
                                    "Fetching SubtreeRoot failed due to server communication problem with " +
                                        "failure: ${response.toThrowable()}"
                                }
                                result = GetSubtreeRootsResult.FailureConnection
                            } else {
                                Twig.error { "Fetching SubtreeRoot failed with failure: ${response.toThrowable()}" }
                                result = GetSubtreeRootsResult.OtherFailure(error)
                            }
                            traceScope.end()
                            throw error
                        }
                    }
                }
                    .filterIsInstance<Response.Success<SubtreeRootUnsafe>>()
                    .map { response ->
                        response.result
                    }
                    .toList()
                    .map {
                        SubtreeRoot.new(it, network)
                    }.let {
                        result =
                            if (it.isEmpty()) {
                                GetSubtreeRootsResult.Linear
                            } else {
                                GetSubtreeRootsResult.SpendBeforeSync(startIndex, it)
                            }
                    }
            }

            traceScope.end()
            return result
        }

        /**
         * Pass the commitment tree data to the database.
         *
         * @param backend Typesafe Rust backend
         * @param startIndex Index to which put the data
         * @param lastValidHeight The height to which rewind in case of any trouble
         * @return PutSaplingSubtreeRootsResult
         */
        @VisibleForTesting
        internal suspend fun putSaplingSubtreeRoots(
            backend: TypesafeBackend,
            startIndex: UInt,
            subTreeRootList: List<SubtreeRoot>,
            lastValidHeight: BlockHeight
        ): PutSaplingSubtreeRootsResult {
            return runCatching {
                backend.putSaplingSubtreeRoots(
                    startIndex = startIndex,
                    roots = subTreeRootList
                )
            }
                .onSuccess {
                    Twig.info {
                        "Sapling subtree roots put successfully with startIndex: $startIndex and roots: " +
                            "${subTreeRootList.size}"
                    }
                }
                .onFailure {
                    Twig.error { "Sapling subtree roots put failed with: $it" }
                }.fold(
                    onSuccess = { PutSaplingSubtreeRootsResult.Success },
                    onFailure = { PutSaplingSubtreeRootsResult.Failure(lastValidHeight, it) }
                )
        }

        /**
         * Notify the wallet of the updated chain tip.
         *
         * @param backend Typesafe Rust backend
         * @param chainTip Height of latest block
         * @param lastValidHeight The height to which rewind in case of any trouble
         * @return UpdateChainTipResult
         */
        @VisibleForTesting
        internal suspend fun updateChainTip(
            backend: TypesafeBackend,
            chainTip: BlockHeight,
            lastValidHeight: BlockHeight
        ): UpdateChainTipResult {
            val traceScope = TraceScope("CompactBlockProcessor.updateChainTip")
            var result =
                runCatching {
                    backend.updateChainTip(chainTip)
                }
                    .onSuccess {
                        Twig.info { "Chain tip updated successfully with height: $chainTip" }
                    }
                    .onFailure {
                        Twig.info { "Chain tip update failed with: $it" }
                    }.fold(
                        onSuccess = { UpdateChainTipResult.Success(chainTip) },
                        onFailure = { UpdateChainTipResult.Failure(lastValidHeight, it) }
                    )
            traceScope.end()
            return result
        }

        /**
         * Get the suggested scan ranges from the wallet database via the rust layer.
         *
         * @param backend Typesafe Rust backend
         * @param lastValidHeight The height to which rewind in case of any trouble
         * @return SuggestScanRangesResult
         */
        @VisibleForTesting
        internal suspend fun suggestScanRanges(
            backend: TypesafeBackend,
            lastValidHeight: BlockHeight
        ): SuggestScanRangesResult {
            val traceScope = TraceScope("CompactBlockProcessor.suggestScanRanges")
            var result =
                runCatching {
                    backend.suggestScanRanges()
                }.onSuccess { ranges ->
                    Twig.info { "Successfully got newly suggested ranges: $ranges" }
                }.onFailure { exception ->
                    Twig.error { "Failed to get newly suggested ranges with: $exception" }
                }.fold(
                    onSuccess = { SuggestScanRangesResult.Success(it) },
                    onFailure = { SuggestScanRangesResult.Failure(lastValidHeight, it) }
                )
            traceScope.end()
            return result
        }

        /**
         * Parse and process ranges. If it recognizes a range with Priority.Verify at the first position, it runs the
         * verification part.
         *
         * @param suggestedRangesResult Wrapper for list of ranges to process
         * @return VerifySuggestedScanRange
         */
        @VisibleForTesting
        @Suppress("MaxLineLength")
        internal fun shouldVerifySuggestedScanRanges(suggestedRangesResult: SuggestScanRangesResult.Success): VerifySuggestedScanRange {
            Twig.debug { "Check for Priority.Verify scan range result: ${suggestedRangesResult.ranges}" }

            return if (suggestedRangesResult.ranges.isEmpty()) {
                VerifySuggestedScanRange.NoRangeToVerify
            } else {
                val firstRangePriority = suggestedRangesResult.ranges[0].getSuggestScanRangePriority()
                if (firstRangePriority == SuggestScanRangePriority.Verify) {
                    VerifySuggestedScanRange.ShouldVerify(suggestedRangesResult.ranges[0])
                } else {
                    VerifySuggestedScanRange.NoRangeToVerify
                }
            }
        }

        /**
         * Get the current wallet summary.
         *
         * @return the latest wallet summary calculated by the Rust layer and wrapped in [GetWalletSummaryResult]
         */
        @VisibleForTesting
        internal suspend fun getWalletSummary(backend: TypesafeBackend): GetWalletSummaryResult {
            return runCatching {
                backend.getWalletSummary()
            }.onSuccess {
                Twig.verbose { "Successfully called getWalletSummary with result: $it" }
            }.onFailure {
                Twig.error { "Failed to call getWalletSummary with result: $it" }
            }.fold(
                onSuccess = {
                    if (it == null) {
                        GetWalletSummaryResult.None
                    } else {
                        GetWalletSummaryResult.Success(it)
                    }
                },
                onFailure = {
                    GetWalletSummaryResult.Failure(it)
                }
            )
        }

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
         * @param enhanceStartHeight the height in which the enhancing should start, or null in case of no previous
         * transaction enhancing done yet
         * @param lastBatchOrder is the order of the last processed batch. It comes from a previous range processing
         * and is necessary for calculating cross ranges batch order of currently processing batches.

         * @return Flow of [BatchSyncProgress] sync and enhancement results
         */
        @VisibleForTesting
        @Suppress("CyclomaticComplexMethod", "LongParameterList", "LongMethod")
        internal suspend fun runSyncingAndEnhancingOnRange(
            backend: TypesafeBackend,
            downloader: CompactBlockDownloader,
            repository: DerivedDataRepository,
            network: ZcashNetwork,
            syncRange: ClosedRange<BlockHeight>,
            withDownload: Boolean,
            enhanceStartHeight: BlockHeight?
        ): Flow<BatchSyncProgress> =
            flow {
                if (syncRange.isEmpty()) {
                    Twig.debug { "No blocks to sync" }
                    emit(
                        BatchSyncProgress(
                            resultState = SyncingResult.AllSuccess
                        )
                    )
                } else {
                    Twig.info { "Syncing blocks in range $syncRange" }

                    val batches = getBatchedBlockList(syncRange, network)

                    // Check for the last enhanced height and eventually set is as the beginning of the next
                    // enhancing range
                    var enhancingRange =
                        if (enhanceStartHeight != null) {
                            BlockHeight(min(syncRange.start.value, enhanceStartHeight.value))..syncRange.start
                        } else {
                            syncRange.start..syncRange.start
                        }

                    batches.asFlow().map {
                        Twig.debug { "Syncing process starts for batch: $it" }

                        // Run downloading stage
                        SyncStageResult(
                            batch = it,
                            stageResult =
                                if (withDownload) {
                                    downloadBatchOfBlocks(
                                        downloader = downloader,
                                        batch = it
                                    )
                                } else {
                                    SyncingResult.DownloadSuccess(null)
                                }
                        )
                    }.buffer(1).map { downloadStageResult ->
                        Twig.debug { "Download stage done with result: $downloadStageResult" }

                        if (downloadStageResult.stageResult !is SyncingResult.DownloadSuccess) {
                            // In case of any failure, we just propagate the result
                            downloadStageResult
                        } else {
                            // Enrich batch model with fetched blocks. It's useful for later blocks deletion
                            downloadStageResult.batch.blocks = downloadStageResult.stageResult.downloadedBlocks

                            // Run scanning stage (which also validates the fetched blocks)
                            SyncStageResult(
                                downloadStageResult.batch,
                                scanBatchOfBlocks(
                                    backend = backend,
                                    batch = downloadStageResult.batch
                                )
                            )
                        }
                    }.map { scanResult ->
                        Twig.debug { "Scan stage done with result: $scanResult" }

                        when (scanResult.stageResult) {
                            is SyncingResult.ScanSuccess -> {
                                // TODO [#1369]: Use the scan summary to trigger balance updates.
                                // TODO [#1369]: https://github.com/Electric-Coin-Company/zcash-android-wallet-sdk/issues/1369
                                val scannedRange = scanResult.stageResult.summary.scannedRange
                                assert(scanResult.batch.range.start <= scannedRange.start)
                                assert(scannedRange.endInclusive <= scanResult.batch.range.endInclusive)

                                // Run deletion stage
                                SyncStageResult(
                                    scanResult.batch,
                                    deleteFilesOfBatchOfBlocks(
                                        downloader = downloader,
                                        batch = scanResult.batch
                                    )
                                )
                            } else -> {
                                scanResult
                            }
                        }
                    }.onEach { continuousResult ->
                        Twig.debug { "Deletion stage done with result: $continuousResult" }

                        var resultState =
                            if (continuousResult.stageResult == SyncingResult.DeleteSuccess) {
                                SyncingResult.AllSuccess
                            } else {
                                continuousResult.stageResult
                            }

                        emit(
                            BatchSyncProgress(
                                order = continuousResult.batch.order,
                                range = continuousResult.batch.range,
                                resultState = resultState
                            )
                        )

                        // Increment and compare the range for triggering the enhancing
                        enhancingRange = enhancingRange.start..continuousResult.batch.range.endInclusive

                        // Enhancing is run when the range is on or over its limit, or there is any failure
                        // from previous stages, or if the end of the sync range is reached.
                        if (enhancingRange.length() >= ENHANCE_BATCH_SIZE ||
                            resultState != SyncingResult.AllSuccess ||
                            continuousResult.batch.order == batches.size.toLong()
                        ) {
                            // Copy the range for use and reset for the next iteration
                            val currentEnhancingRange = enhancingRange
                            enhancingRange = enhancingRange.endInclusive..enhancingRange.endInclusive
                            enhanceTransactionDetails(
                                range = currentEnhancingRange,
                                repository = repository,
                                backend = backend,
                                downloader = downloader
                            ).collect { enhancingResult ->
                                Twig.info { "Enhancing result: $enhancingResult" }
                                resultState =
                                    when (enhancingResult) {
                                        is SyncingResult.UpdateBirthday -> {
                                            Twig.info { "Birthday height update reporting" }
                                            enhancingResult
                                        }
                                        is SyncingResult.EnhanceFailed -> {
                                            Twig.error { "Enhancing failed for: $enhancingRange with $enhancingResult" }
                                            enhancingResult
                                        }
                                        else -> {
                                            // Transactions enhanced correctly. Let's continue with block processing.
                                            enhancingResult
                                        }
                                    }
                                emit(
                                    BatchSyncProgress(
                                        order = continuousResult.batch.order,
                                        range = continuousResult.batch.range,
                                        resultState = resultState
                                    )
                                )
                            }
                        }
                        Twig.info {
                            "All sync stages done for the batch ${continuousResult.batch.order}/${batches.size}:" +
                                " ${continuousResult.batch} with result state: $resultState"
                        }
                    }.takeWhile { batchProcessResult ->
                        batchProcessResult.stageResult == SyncingResult.DeleteSuccess ||
                            batchProcessResult.stageResult == SyncingResult.UpdateBirthday
                    }.collect()
                }
            }

        /**
         * Returns count of batches of blocks across all ranges. It works the same when triggered from the Linear
         * synchronization or from the SbS synchronization.
         *
         * @param syncRanges List of ranges of all blocks to process
         *
         * @return Count of all batches for processing
         */
        private fun getBatchCount(syncRanges: List<ClosedRange<BlockHeight>>): Long {
            var allRangesBatchCount = 0L
            var allMissingBlocksCount = 0L

            syncRanges.forEach { range ->
                val missingBlockCount = range.endInclusive.value - range.start.value + 1
                val batchCount = (missingBlockCount + SYNC_BATCH_SIZE - 1) / SYNC_BATCH_SIZE
                allMissingBlocksCount += missingBlockCount
                allRangesBatchCount += batchCount
            }

            Twig.debug {
                "Found $allMissingBlocksCount missing blocks, syncing in $allRangesBatchCount batches of " +
                    "$SYNC_BATCH_SIZE..."
            }
            return allRangesBatchCount
        }

        /**
         * Prepare list of all [BlockBatch] internal objects to be processed during a range of
         * blocks processing
         *
         * @param syncRange Current range to be processed
         * @param network The network we are operating on
         *
         * @return List of [BlockBatch] to prepare for synchronization
         */
        private fun getBatchedBlockList(
            syncRange: ClosedRange<BlockHeight>,
            network: ZcashNetwork
        ): List<BlockBatch> {
            val batchCount = getBatchCount(listOf(syncRange))
            var start = syncRange.start
            return buildList {
                for (index in 1..batchCount) {
                    val end =
                        BlockHeight.new(
                            network,
                            min(
                                (syncRange.start.value + (index * SYNC_BATCH_SIZE)) - 1,
                                syncRange.endInclusive.value
                            )
                        ) // subtract 1 on the first value because the range is inclusive

                    add(
                        BlockBatch(
                            order = index,
                            range = start..end
                        )
                    )
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
        internal suspend fun downloadBatchOfBlocks(
            downloader: CompactBlockDownloader,
            batch: BlockBatch
        ): SyncingResult {
            val traceScope = TraceScope("CompactBlockProcessor.downloadBatchOfBlocks")
            var downloadedBlocks = listOf<JniBlockMeta>()
            var downloadException: CompactBlockProcessorException.FailedDownloadException? = null

            retryUpToAndContinue(
                retries = RETRIES,
                exceptionWrapper = {
                    downloadException = CompactBlockProcessorException.FailedDownloadException(it)
                    downloadException!!
                }
            ) { failedAttempts ->
                if (failedAttempts == 0) {
                    Twig.verbose { "Starting to download batch $batch" }
                } else {
                    Twig.warn { "Retrying to download batch $batch after $failedAttempts failure(s)..." }
                }
                downloadedBlocks = downloader.downloadBlockRange(batch.range)
            }
            traceScope.end()
            Twig.verbose { "Successfully downloaded batch: $batch of $downloadedBlocks blocks" }

            return if (downloadedBlocks.isNotEmpty()) {
                SyncingResult.DownloadSuccess(downloadedBlocks)
            } else {
                SyncingResult.DownloadFailed(
                    batch.range.start,
                    downloadException ?: CompactBlockProcessorException.FailedDownloadException()
                )
            }
        }

        @VisibleForTesting
        internal suspend fun scanBatchOfBlocks(
            batch: BlockBatch,
            backend: TypesafeBackend
        ): SyncingResult {
            val traceScope = TraceScope("CompactBlockProcessor.scanBatchOfBlocks")
            var result =
                runCatching {
                    backend.scanBlocks(batch.range.start, batch.range.length())
                }.onSuccess {
                    Twig.verbose { "Successfully scanned batch $batch" }
                }.onFailure {
                    Twig.error { "Failed while scanning batch $batch with $it" }
                }.fold(
                    onSuccess = { SyncingResult.ScanSuccess(it) },
                    onFailure = {
                        // Check if the error is continuity type
                        if (it.isScanContinuityError()) {
                            SyncingResult.ContinuityError(
                                // To ensure we later rewind below the failed height
                                failedAtHeight = batch.range.start - 1,
                                exception = CompactBlockProcessorException.FailedScanException(it)
                            )
                        } else {
                            SyncingResult.ScanFailed(
                                failedAtHeight = batch.range.start,
                                exception = CompactBlockProcessorException.FailedScanException(it)
                            )
                        }
                    }
                )
            traceScope.end()
            return result
        }

        @VisibleForTesting
        internal suspend fun deleteAllBlockFiles(
            downloader: CompactBlockDownloader,
            lastKnownHeight: BlockHeight?
        ): SyncingResult {
            Twig.verbose { "Starting to delete all temporary block files" }
            val traceScope = TraceScope("CompactBlockProcessor.deleteAllBlockFiles")
            var result =
                if (downloader.compactBlockRepository.deleteAllCompactBlockFiles()) {
                    Twig.verbose { "Successfully deleted all temporary block files" }
                    SyncingResult.DeleteSuccess
                } else {
                    SyncingResult.DeleteFailed(
                        lastKnownHeight,
                        CompactBlockProcessorException.FailedDeleteException()
                    )
                }
            traceScope.end()
            return result
        }

        @VisibleForTesting
        internal suspend fun deleteFilesOfBatchOfBlocks(
            batch: BlockBatch,
            downloader: CompactBlockDownloader
        ): SyncingResult {
            Twig.verbose { "Starting to delete temporary block files from batch: $batch" }
            val traceScope = TraceScope("CompactBlockProcessor.deleteFilesOfBatchOfBlocks")

            var result =
                batch.blocks?.let { blocks ->
                    val deleted = downloader.compactBlockRepository.deleteCompactBlockFiles(blocks)
                    if (deleted) {
                        Twig.verbose { "Successfully deleted all temporary batched block files" }
                        SyncingResult.DeleteSuccess
                    } else {
                        SyncingResult.DeleteFailed(
                            batch.range.start,
                            CompactBlockProcessorException.FailedDeleteException()
                        )
                    }
                } ?: SyncingResult.DeleteSuccess
            traceScope.end()
            return result
        }

        @VisibleForTesting
        internal suspend fun enhanceTransactionDetails(
            range: ClosedRange<BlockHeight>,
            repository: DerivedDataRepository,
            backend: TypesafeBackend,
            downloader: CompactBlockDownloader
        ): Flow<SyncingResult> =
            flow {
                Twig.debug { "Enhancing transaction details for blocks $range" }

                val newTxs = repository.findNewTransactions(range)
                if (newTxs.isEmpty()) {
                    Twig.debug { "No new transactions found in $range" }
                } else {
                    Twig.debug { "Enhancing ${newTxs.size} transaction(s)!" }

                    // If the first transaction has been added
                    if (newTxs.size.toLong() == repository.getTransactionCount()) {
                        Twig.debug { "Encountered the first transaction. This changes the birthday height!" }
                        emit(SyncingResult.UpdateBirthday)
                    }

                    newTxs.filter { it.minedHeight != null }.onEach { newTransaction ->
                        val trEnhanceResult = enhanceTransaction(newTransaction, backend, downloader)
                        if (trEnhanceResult is SyncingResult.EnhanceFailed) {
                            Twig.error { "Encountered transaction enhancing error: ${trEnhanceResult.exception}" }
                            emit(trEnhanceResult)
                            // We intentionally do not terminate the batch enhancing here, just reporting it
                        }
                    }
                }

                Twig.debug { "Done enhancing transaction details" }
                emit(SyncingResult.EnhanceSuccess)
            }

        private suspend fun enhanceTransaction(
            transaction: DbTransactionOverview,
            backend: TypesafeBackend,
            downloader: CompactBlockDownloader
        ): SyncingResult {
            Twig.debug {
                "Starting enhancing transaction (txid:${transaction.txIdString()}  block:${transaction
                    .minedHeight})"
            }
            if (transaction.minedHeight == null) {
                return SyncingResult.EnhanceSuccess
            }

            val traceScope = TraceScope("CompactBlockProcessor.enhanceTransaction")
            var result =
                try {
                    // Fetching transaction is done with retries to eliminate a bad network condition
                    Twig.verbose {
                        "Fetching transaction (txid:${transaction.txIdString()}  block:${transaction
                            .minedHeight})"
                    }
                    val transactionData =
                        fetchTransaction(
                            transactionId = transaction.txIdString(),
                            rawTransactionId = transaction.rawId.byteArray,
                            minedHeight = transaction.minedHeight,
                            downloader = downloader
                        )

                    // Decrypting and storing transaction is run just once, since we consider it more stable
                    Twig.verbose {
                        "Decrypting and storing transaction " +
                            "(txid:${transaction.txIdString()}  block:${transaction.minedHeight})"
                    }
                    decryptTransaction(
                        transactionData = transactionData,
                        minedHeight = transaction.minedHeight,
                        backend = backend
                    )

                    Twig.debug {
                        "Done enhancing transaction (txid:${transaction.txIdString()} block:${transaction
                            .minedHeight})"
                    }
                    SyncingResult.EnhanceSuccess
                } catch (exception: CompactBlockProcessorException.EnhanceTransactionError) {
                    SyncingResult.EnhanceFailed(
                        transaction.minedHeight,
                        exception
                    )
                }
            traceScope.end()
            return result
        }

        // TODO [#1254]: CompactblockProcessor.fetchTransaction pass txId twice
        // TODO [#1254]: https://github.com/zcash/zcash-android-wallet-sdk/issues/1254
        @Throws(EnhanceTxDownloadError::class)
        private suspend fun fetchTransaction(
            transactionId: String,
            rawTransactionId: ByteArray,
            minedHeight: BlockHeight,
            downloader: CompactBlockDownloader
        ): ByteArray {
            val traceScope = TraceScope("CompactBlockProcessor.fetchTransaction")
            var transactionDataResult: ByteArray? = null
            retryUpToAndThrow(TRANSACTION_FETCH_RETRIES) { failedAttempts ->
                if (failedAttempts == 0) {
                    Twig.debug { "Starting to fetch transaction (txid:$transactionId, block:$minedHeight)" }
                } else {
                    Twig.warn {
                        "Retrying to fetch transaction (txid:$transactionId, block:$minedHeight) after" +
                            " $failedAttempts failure(s)..."
                    }
                }
                when (val response = downloader.fetchTransaction(rawTransactionId)) {
                    is Response.Success -> {
                        transactionDataResult = response.result.data
                    }
                    is Response.Failure -> {
                        throw EnhanceTxDownloadError(minedHeight, response.toThrowable())
                    }
                }
            }
            traceScope.end()
            // Result is fetched or EnhanceTxDownloadError is thrown after all attempts failed at this point
            return transactionDataResult!!
        }

        @Throws(EnhanceTxDecryptError::class)
        private suspend fun decryptTransaction(
            transactionData: ByteArray,
            minedHeight: BlockHeight,
            backend: TypesafeBackend,
        ) {
            val traceScope = TraceScope("CompactBlockProcessor.decryptTransaction")
            runCatching {
                backend.decryptAndStoreTransaction(transactionData)
            }.onFailure {
                traceScope.end()
                throw EnhanceTxDecryptError(minedHeight, it)
            }
            traceScope.end()
        }

        /**
         * Get the height of the last block that was scanned by this processor.
         *
         * @return the last scanned height reported by the repository.
         */
        @VisibleForTesting
        internal suspend fun getMaxScannedHeight(backend: TypesafeBackend): GetMaxScannedHeightResult {
            return runCatching {
                backend.getMaxScannedHeight()
            }.onSuccess {
                Twig.verbose { "Successfully called getMaxScannedHeight with result: $it" }
            }.onFailure {
                Twig.error { "Failed to call getMaxScannedHeight with result: $it" }
            }.fold(
                onSuccess = {
                    if (it == null) {
                        GetMaxScannedHeightResult.None
                    } else {
                        GetMaxScannedHeightResult.Success(it)
                    }
                },
                onFailure = {
                    GetMaxScannedHeightResult.Failure(it)
                }
            )
        }

        /**
         * Get the height of the first un-enhanced transaction detail from the repository.
         *
         * @return the oldest transaction which hasn't been enhanced yet, or null in case of all transaction enhanced
         * or repository is empty
         */
        @VisibleForTesting
        @Suppress("MaxLineLength")
        internal suspend fun getFirstUnenhancedHeight(repository: DerivedDataRepository) = repository.firstUnenhancedHeight()

        /**
         * Get the height of the last block that was downloaded by this processor.
         *
         * @return the last downloaded height reported by the downloader.
         */
        @Suppress("MaxLineLength")
        internal suspend fun getLastDownloadedHeight(downloader: CompactBlockDownloader) = downloader.getLastDownloadedHeight()

        /**
         * Get the current unified address for the given wallet account.
         *
         * @return the current unified address of this account.
         */
        internal suspend fun getCurrentAddress(
            backend: TypesafeBackend,
            account: Account
        ) = backend.getCurrentAddress(account)

        /**
         * Get the legacy Sapling address corresponding to the current unified address for the given wallet account.
         *
         * @return a Sapling address.
         */
        internal suspend fun getLegacySaplingAddress(
            backend: TypesafeBackend,
            account: Account
        ) = backend.getSaplingReceiver(
            backend.getCurrentAddress(account)
        )
            ?: throw InitializeException.MissingAddressException("legacy Sapling")

        /**
         * Get the legacy transparent address corresponding to the current unified address for the given wallet account.
         *
         * @return a transparent address.
         */
        internal suspend fun getTransparentAddress(
            backend: TypesafeBackend,
            account: Account
        ) = backend.getTransparentReceiver(
            backend.getCurrentAddress(account)
        )
            ?: throw InitializeException.MissingAddressException("legacy transparent")
    }

    /**
     * Sets new values of ProcessorInfo and network height corresponding to the provided data for this
     * [CompactBlockProcessor].
     *
     * @param networkBlockHeight the latest block available to lightwalletd that may or may not be
     * downloaded by this wallet yet.
     * @param overallSyncRange the inclusive range to sync. This represents what we most recently
     * wanted to sync. In most cases, it will be an invalid range because we'd like to sync blocks
     * that we don't yet have.
     * @param firstUnenhancedHeight the height at which the enhancing should start. Use null if you have no
     * preferences. The height will be calculated automatically for you to continue where it previously ended, or
     * it'll be set to the sync start height in case of the first sync attempt.
     */
    private fun setProcessorInfo(
        networkBlockHeight: BlockHeight? = _processorInfo.value.networkBlockHeight,
        overallSyncRange: ClosedRange<BlockHeight>? = _processorInfo.value.overallSyncRange,
        firstUnenhancedHeight: BlockHeight? = _processorInfo.value.firstUnenhancedHeight,
    ) {
        _networkHeight.value = networkBlockHeight
        _processorInfo.value =
            ProcessorInfo(
                networkBlockHeight = networkBlockHeight,
                overallSyncRange = overallSyncRange,
                firstUnenhancedHeight = firstUnenhancedHeight
            )
    }

    /**
     * Sets the progress for this [CompactBlockProcessor].
     *
     * @param progress the block syncing progress of type [PercentDecimal] in the range of [0, 1]
     */
    private fun setProgress(progress: PercentDecimal = _progress.value) {
        _progress.value = progress
    }

    /**
     * Sets the state of this [CompactBlockProcessor].
     */
    private suspend fun setState(newState: State) {
        _state.value = newState
    }

    private suspend fun handleChainError(errorHeight: BlockHeight?) {
        printValidationErrorInfo(errorHeight)
        errorHeight?.let {
            determineLowerBound(errorHeight).let { lowerBound ->
                Twig.warn { "Handling chain error at $errorHeight by rewinding to block $lowerBound" }
                onChainErrorListener?.invoke(errorHeight, lowerBound)
                rewindToNearestHeight(lowerBound)
            }
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
            // This could create an invalid height if height was saplingActivationHeight
            val rewindHeight = BlockHeight(height.value - 1)
            backend.getNearestRewindHeight(rewindHeight)
        }
    }

    /**
     * Rewind back two weeks worth of blocks. The final amount of rewinded blocks depends on [rewindToNearestHeight].
     */
    suspend fun quickRewind(): Boolean {
        val height =
            when (val result = getMaxScannedHeight(backend)) {
                is GetMaxScannedHeightResult.Success -> result.height
                else -> return false
            }
        val blocksPer14Days = 14.days.inWholeMilliseconds / ZcashSdk.BLOCK_INTERVAL_MILLIS.toInt()
        val twoWeeksBack =
            BlockHeight.new(
                network,
                (height.value - blocksPer14Days).coerceAtLeast(lowerBoundHeight.value)
            )
        return rewindToNearestHeight(twoWeeksBack)
    }

    @Suppress("LongMethod")
    suspend fun rewindToNearestHeight(height: BlockHeight): Boolean {
        processingMutex.withLockLogged("rewindToHeight") {
            val lastLocalBlock =
                when (val result = getMaxScannedHeight(backend)) {
                    is GetMaxScannedHeightResult.Success -> result.height
                    else -> return false
                }
            val targetHeight = getNearestRewindHeight(height)

            Twig.debug {
                "Rewinding to requested height: $height using target height: $targetHeight with last local block:" +
                    " $lastLocalBlock"
            }

            if (targetHeight < lastLocalBlock) {
                Twig.debug { "Rewinding because targetHeight is less than lastLocalBlock." }
                runCatching {
                    backend.rewindToHeight(targetHeight)
                    downloader.rewindToHeight(targetHeight)
                }.onFailure {
                    Twig.error { "Rewinding to the targetHeight $targetHeight failed with $it" }
                }.onSuccess {
                    Twig.info { "Rewind to $targetHeight was successful." }
                    setState(newState = State.Syncing)
                    setProgress(progress = PercentDecimal.ZERO_PERCENT)
                    setProcessorInfo(overallSyncRange = null)
                }
            } else {
                Twig.info {
                    "Not rewinding dataDb because last local block is $lastLocalBlock which is less than the target " +
                        "height of $targetHeight"
                }
            }
        }
        return true
    }

    /** insightful function for debugging these critical errors */
    private suspend fun printValidationErrorInfo(
        errorHeight: BlockHeight?,
        count: Int = 11
    ) {
        // Note: blocks are public information so it's okay to print them but, still, let's not unless we're
        // debugging something
        if (!BuildConfig.DEBUG) {
            return
        }

        if (errorHeight == null) {
            Twig.debug { "Validation failed at unspecified block height" }
            return
        }

        var errorInfo = ValidationErrorInfo(errorHeight)
        Twig.debug { "Validation failed at block ${errorInfo.errorHeight}" }

        errorInfo = ValidationErrorInfo(errorHeight + 1)
        Twig.debug { "The next block is ${errorInfo.errorHeight}" }

        Twig.debug { "=================== BLOCKS [$errorHeight..${errorHeight.value + count - 1}]: START ========" }
        repeat(count) { i ->
            val height = errorHeight + i
            val block = downloader.compactBlockRepository.findCompactBlock(height)
            Twig.debug { "block: $height\thash=${block?.hash?.toHexReversed()}" }
        }
        Twig.debug { "=================== BLOCKS [$errorHeight..${errorHeight.value + count - 1}]: END ========" }
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
        val errorCount = consecutiveChainErrors.incrementAndGet()
        val offset = min(MAX_REORG_SIZE, REWIND_DISTANCE * errorCount)
        return BlockHeight(max(errorHeight.value - offset, lowerBoundHeight.value)).also {
            Twig.debug {
                "offset = min($MAX_REORG_SIZE, $REWIND_DISTANCE * $errorCount) = " +
                    "$offset"
            }
            Twig.debug { "lowerBound = max($errorHeight - $offset, $lowerBoundHeight) = $it" }
        }
    }

    /**
     * Poll on time boundaries. Per Issue #95, we want to avoid exposing computation time to a
     * network observer. Instead, we poll at regular time intervals that are large enough for all
     * computation to complete so no intervals are skipped. See 95 for more details.
     *
     * @param fastIntervalDesired set if the short poll interval should be used
     *
     * @return the duration in milliseconds to the next poll attempt
     */
    private fun calculatePollInterval(fastIntervalDesired: Boolean = false): Duration {
        val interval =
            if (fastIntervalDesired) {
                POLL_INTERVAL_SHORT
            } else {
                POLL_INTERVAL
            }
        val now = System.currentTimeMillis()
        val deltaToNextInterval = interval - (now + interval).rem(interval)
        return deltaToNextInterval.toDuration(DurationUnit.MILLISECONDS)
    }

    private suspend fun takeANap(
        summary: String,
        fastIntervalDesired: Boolean
    ) {
        val napTime = calculatePollInterval(fastIntervalDesired)
        Twig.info {
            "$summary Sleeping for ${napTime}ms " +
                "(latest height: ${_processorInfo.value.networkBlockHeight})."
        }
        delay(napTime)
    }

    suspend fun calculateBirthdayHeight(): BlockHeight {
        return repository.getOldestTransaction()?.minedHeight?.value?.let {
            // To be safe adjust for reorgs (and generally a little cushion is good for privacy), so we round down to
            // the nearest 100 and then subtract 100 to ensure that the result is always at least 100 blocks away
            var oldestTransactionHeightValue = it
            oldestTransactionHeightValue -= oldestTransactionHeightValue.rem(MAX_REORG_SIZE) - MAX_REORG_SIZE.toLong()
            if (oldestTransactionHeightValue < lowerBoundHeight.value) {
                lowerBoundHeight
            } else {
                BlockHeight.new(network, oldestTransactionHeightValue)
            }
        } ?: lowerBoundHeight
    }

    suspend fun getUtxoCacheBalance(address: String): WalletBalance = backend.getDownloadedUtxoBalance(address)

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
         * **Scanning** is when the blocks that have been downloaded are actively being decrypted and validated to
         * ensure that there are no gaps and that every block is chain-sequential to the previous block, which
         * determines whether a reorg has happened on our watch.
         *
         * **Deleting** is when the temporary block files being removed from the persistence.
         *
         * **Enhancing** is when transaction details are being retrieved. This typically means the wallet has
         * downloaded and scanned blocks and is now processing any transactions that were discovered. Once a
         * transaction is discovered, followup network requests are needed in order to retrieve memos or outbound
         * transaction information, like the recipient address. The existing information we have about transactions
         * is enhanced by the new information.
         */
        object Syncing : IConnected, ISyncing, State()

        /**
         * [State] for when we are done with syncing the blocks, for now, i.e. all necessary stages done (download,
         * scan).
         */
        class Synced(val syncedRange: ClosedRange<BlockHeight>?) : IConnected, ISyncing, State()

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
     * @param overallSyncRange inclusive range to sync. Meaning, if the range is 10..10,
     * then we will download exactly block 10. If the range is 11..10, then we want to download
     * block 11 but can't.
     * @param firstUnenhancedHeight the height in which the enhancing should start, or null in case of no previous
     * transaction enhancing done yet
     */
    data class ProcessorInfo(
        val networkBlockHeight: BlockHeight?,
        val overallSyncRange: ClosedRange<BlockHeight>?,
        val firstUnenhancedHeight: BlockHeight?
    )

    data class ValidationErrorInfo(
        val errorHeight: BlockHeight
    )

    //
    // Helper Extensions
    //

    /**
     * Log the mutex in great detail just in case we need it for troubleshooting deadlock.
     */
    private suspend inline fun <T> Mutex.withLockLogged(
        name: String,
        block: () -> T
    ): T {
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
    fun String.toId() =
        lowercase(Locale.ROOT).run {
            when {
                contains("main") -> "mainnet"
                contains("test") -> "testnet"
                else -> this
            }
        }
    return chainName.toId() == network.toId()
}
