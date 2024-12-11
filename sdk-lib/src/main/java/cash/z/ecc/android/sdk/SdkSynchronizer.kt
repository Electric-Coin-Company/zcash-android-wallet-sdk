package cash.z.ecc.android.sdk

import android.content.Context
import cash.z.ecc.android.sdk.Synchronizer.Status.DISCONNECTED
import cash.z.ecc.android.sdk.Synchronizer.Status.STOPPED
import cash.z.ecc.android.sdk.Synchronizer.Status.SYNCED
import cash.z.ecc.android.sdk.Synchronizer.Status.SYNCING
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor.State.Disconnected
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor.State.Initialized
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor.State.Stopped
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor.State.Synced
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor.State.Syncing
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException
import cash.z.ecc.android.sdk.exception.InitializeException
import cash.z.ecc.android.sdk.exception.TransactionEncoderException
import cash.z.ecc.android.sdk.ext.ConsensusBranchId
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.FastestServerFetcher
import cash.z.ecc.android.sdk.internal.SaplingParamTool
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.TypesafeBackend
import cash.z.ecc.android.sdk.internal.TypesafeBackendImpl
import cash.z.ecc.android.sdk.internal.block.CompactBlockDownloader
import cash.z.ecc.android.sdk.internal.db.DatabaseCoordinator
import cash.z.ecc.android.sdk.internal.db.derived.DbDerivedDataRepository
import cash.z.ecc.android.sdk.internal.db.derived.DerivedDataDb
import cash.z.ecc.android.sdk.internal.exchange.UsdExchangeRateFetcher
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.isNullOrEmpty
import cash.z.ecc.android.sdk.internal.ext.tryNull
import cash.z.ecc.android.sdk.internal.jni.RustBackend
import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.internal.model.TreeState
import cash.z.ecc.android.sdk.internal.model.ZcashProtocol
import cash.z.ecc.android.sdk.internal.model.ext.toBlockHeight
import cash.z.ecc.android.sdk.internal.repository.CompactBlockRepository
import cash.z.ecc.android.sdk.internal.repository.DerivedDataRepository
import cash.z.ecc.android.sdk.internal.storage.block.FileCompactBlockRepository
import cash.z.ecc.android.sdk.internal.transaction.OutboundTransactionManager
import cash.z.ecc.android.sdk.internal.transaction.OutboundTransactionManagerImpl
import cash.z.ecc.android.sdk.internal.transaction.TransactionEncoder
import cash.z.ecc.android.sdk.internal.transaction.TransactionEncoderImpl
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountCreateSetup
import cash.z.ecc.android.sdk.model.AccountImportSetup
import cash.z.ecc.android.sdk.model.AccountPurpose
import cash.z.ecc.android.sdk.model.AccountUsk
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FastestServersResult
import cash.z.ecc.android.sdk.model.FetchFiatCurrencyResult
import cash.z.ecc.android.sdk.model.ObserveFiatCurrencyResult
import cash.z.ecc.android.sdk.model.PercentDecimal
import cash.z.ecc.android.sdk.model.Proposal
import cash.z.ecc.android.sdk.model.TransactionOutput
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.TransactionPool
import cash.z.ecc.android.sdk.model.TransactionRecipient
import cash.z.ecc.android.sdk.model.TransactionSubmitResult
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.CheckpointTool
import cash.z.ecc.android.sdk.type.AddressType
import cash.z.ecc.android.sdk.type.AddressType.Shielded
import cash.z.ecc.android.sdk.type.AddressType.Tex
import cash.z.ecc.android.sdk.type.AddressType.Transparent
import cash.z.ecc.android.sdk.type.AddressType.Unified
import cash.z.ecc.android.sdk.type.ConsensusMatchType
import cash.z.ecc.android.sdk.type.ServerValidation
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.new
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

/**
 * A Synchronizer that attempts to remain operational, despite any number of errors that can occur.
 * It acts as the glue that ties all the pieces of the SDK together. Each component of the SDK is
 * designed for the potential of stand-alone usage but coordinating all the interactions is non-
 * trivial. So the Synchronizer facilitates this, acting as reference that demonstrates how all the
 * pieces can be tied together. Its goal is to allow a developer to focus on their app rather than
 * the nuances of how Zcash works.
 *
 * @property synchronizerKey Identifies the synchronizer's on-disk state
 * @property storage exposes flows of wallet transaction information.
 * @property txManager manages and tracks outbound transactions.
 * @property processor saves the downloaded compact blocks to the cache and then scans those blocks for
 * data related to this wallet.
 */
@Suppress("TooManyFunctions", "LongParameterList")
class SdkSynchronizer private constructor(
    private val context: Context,
    private val synchronizerKey: SynchronizerKey,
    private val storage: DerivedDataRepository,
    private val txManager: OutboundTransactionManager,
    val processor: CompactBlockProcessor,
    private val backend: TypesafeBackend,
    private val fetchFastestServers: FastestServerFetcher,
    private val fetchExchangeChangeUsd: UsdExchangeRateFetcher
) : CloseableSynchronizer {
    companion object {
        private sealed class InstanceState {
            object Active : InstanceState()

            data class ShuttingDown(val job: Job) : InstanceState()
        }

        private val instances: MutableMap<SynchronizerKey, InstanceState> =
            ConcurrentHashMap<SynchronizerKey, InstanceState>()

        private val mutex = Mutex()

        /**
         * Convenience method to create new SdkSynchronizer instance.
         *
         * @return Synchronizer instance as CloseableSynchronizer
         *
         * @throws IllegalStateException If multiple instances of synchronizer with the same network+alias are
         * active at the same time.  Call `close` to finish one synchronizer before starting another one with the same
         * network+alias.
         */
        @Suppress("LongParameterList")
        internal suspend fun new(
            context: Context,
            zcashNetwork: ZcashNetwork,
            alias: String,
            repository: DerivedDataRepository,
            txManager: OutboundTransactionManager,
            processor: CompactBlockProcessor,
            backend: TypesafeBackend,
            fastestServerFetcher: FastestServerFetcher,
            fetchExchangeChangeUsd: UsdExchangeRateFetcher,
        ): CloseableSynchronizer {
            val synchronizerKey = SynchronizerKey(zcashNetwork, alias)

            return mutex.withLock {
                waitForShutdown(synchronizerKey)
                checkForExistingSynchronizers(synchronizerKey)

                SdkSynchronizer(
                    context,
                    synchronizerKey,
                    repository,
                    txManager,
                    processor,
                    backend,
                    fastestServerFetcher,
                    fetchExchangeChangeUsd
                ).apply {
                    instances[synchronizerKey] = InstanceState.Active
                    start()
                }
            }
        }

        private suspend fun waitForShutdown(synchronizerKey: SynchronizerKey) {
            instances[synchronizerKey]?.let {
                if (it is InstanceState.ShuttingDown) {
                    Twig.debug { "Waiting for prior synchronizer instance to shut down" } // $NON-NLS-1$
                    it.job.join()
                }
            }
        }

        private fun checkForExistingSynchronizers(synchronizerKey: SynchronizerKey) {
            check(!instances.containsKey(synchronizerKey)) {
                "Another synchronizer with $synchronizerKey is currently active" // $NON-NLS-1$
            }
        }

        internal suspend fun erase(
            appContext: Context,
            network: ZcashNetwork,
            alias: String
        ): Boolean {
            val key = SynchronizerKey(network, alias)

            return mutex.withLock {
                waitForShutdown(key)
                checkForExistingSynchronizers(key)

                DatabaseCoordinator.getInstance(appContext).deleteDatabases(network, alias)
            }
        }
    }

    private val _status = MutableStateFlow(DISCONNECTED)

    val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val walletBalances = processor.walletBalances.asStateFlow()

    private val refreshExchangeRateUsd = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    private var lastExchangeRateValue = ObserveFiatCurrencyResult()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val exchangeRateUsd =
        channelFlow {
            refreshExchangeRateUsd
                .flatMapLatest {
                    flow {
                        emit(lastExchangeRateValue.copy(isLoading = true))
                        lastExchangeRateValue =
                            when (val result = fetchExchangeChangeUsd()) {
                                is FetchFiatCurrencyResult.Error -> lastExchangeRateValue.copy(isLoading = false)

                                is FetchFiatCurrencyResult.Success ->
                                    lastExchangeRateValue.copy(
                                        isLoading = false,
                                        currencyConversion = result.currencyConversion
                                    )
                            }
                        emit(lastExchangeRateValue)
                    }
                }
                .onEach { send(it) }
                .flowOn(Dispatchers.Default)
                .launchIn(this)

            awaitClose {
                // do nothing
            }
        }.flowOn(Dispatchers.Default).stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(), // stop immediately
            initialValue = ObserveFiatCurrencyResult()
        )

    override val transactions
        get() =
            combine(processor.networkHeight, storage.allTransactions) { networkHeight, allTransactions ->
                val latestBlockHeight =
                    networkHeight ?: runCatching {
                        backend.getMaxScannedHeight()
                    }.onFailure {
                        Twig.error(it) { "Failed to get max scanned height" }
                    }.getOrNull()

                allTransactions.map { TransactionOverview.new(it, latestBlockHeight) }
            }

    override val network: ZcashNetwork get() = processor.network

    /**
     * Indicates the status of this Synchronizer. This implementation basically simplifies the
     * status of the processor to focus only on the high level states that matter most. Whenever the
     * processor is finished scanning, the synchronizer updates transaction and balance info and
     * then emits a [SYNCED] status.
     */
    override val status = _status.asStateFlow()

    /**
     * Indicates the download progress of the Synchronizer. When progress reaches `PercentDecimal.ONE_HUNDRED_PERCENT`,
     * that signals that the Synchronizer is in sync with the network. Balances should be considered
     * inaccurate and outbound transactions should be prevented until this sync is complete. It is
     * a simplified version of [processorInfo].
     */
    override val progress: Flow<PercentDecimal> = processor.progress

    /**
     * Indicates the latest information about the blocks that have been processed by the SDK. This
     * is very helpful for conveying detailed progress and status to the user.
     */
    override val processorInfo: Flow<CompactBlockProcessor.ProcessorInfo> = processor.processorInfo

    /**
     * The latest height seen on the network while processing blocks. This may differ from the
     * latest height scanned and is useful for determining block confirmations and expiration.
     */
    override val networkHeight: StateFlow<BlockHeight?> = processor.networkHeight

    //
    // Error Handling
    //

    /**
     * A callback to invoke whenever an uncaught error is encountered. By definition, the return
     * value of the function is ignored because this error is unrecoverable. The only reason the
     * function has a return value is so that all error handlers work with the same signature which
     * allows one function to handle all errors in simple apps. This callback is not called on the
     * main thread so any UI work would need to switch context to the main thread.
     */
    override var onCriticalErrorHandler: ((Throwable?) -> Boolean)? = null

    /**
     * A callback to invoke whenever a processor error is encountered. Returning true signals that
     * the error was handled and a retry attempt should be made, if possible. This callback is not
     * called on the main thread so any UI work would need to switch context to the main thread.
     */
    override var onProcessorErrorHandler: ((Throwable?) -> Boolean)? = null

    /**
     * A callback to invoke whenever a server error is encountered while submitting a transaction to
     * lightwalletd. Returning true signals that the error was handled and a retry attempt should be
     * made, if possible. This callback is not called on the main thread so any UI work would need
     * to switch context to the main thread.
     */
    override var onSubmissionErrorHandler: ((Throwable?) -> Boolean)? = null

    /**
     * A callback to invoke whenever a processor is not setup correctly. Returning true signals that
     * the invalid setup should be ignored. If no handler is set, then any setup error will result
     * in a critical error. This callback is not called on the main thread so any UI work would need
     * to switch context to the main thread.
     */
    override var onSetupErrorHandler: ((Throwable?) -> Boolean)? = null

    /**
     * A callback to invoke whenever a chain error is encountered. These occur whenever the
     * processor detects a missing or non-chain-sequential block (i.e. a reorg).
     */
    override var onChainErrorHandler: ((errorHeight: BlockHeight, rewindHeight: BlockHeight) -> Any)? = null

    //
    // Public API
    //

    /**
     * Convenience function for the latest height. Specifically, this value represents the last
     * height that the synchronizer has observed from the lightwalletd server. Instead of using
     * this, a wallet will more likely want to consume the flow of processor info using
     * [processorInfo].
     */
    override val latestHeight
        get() = processor.processorInfo.value.networkBlockHeight

    override val latestBirthdayHeight
        get() = processor.birthdayHeight

    override suspend fun getFastestServers(
        context: Context,
        servers: List<LightWalletEndpoint>
    ): Flow<FastestServersResult> = fetchFastestServers(context, servers)

    internal fun start() {
        coroutineScope.onReady()
    }

    override fun close() {
        // Note that stopping will continue asynchronously.  Race conditions with starting a new synchronizer are
        // avoided with a delay during startup.

        val shutdownJob =
            coroutineScope.launch {
                Twig.info { "Stopping synchronizer $synchronizerKey…" }
                processor.stop()
                fetchExchangeChangeUsd.dispose()
            }

        instances[synchronizerKey] = InstanceState.ShuttingDown(shutdownJob)

        shutdownJob.invokeOnCompletion {
            coroutineScope.cancel()
            _status.value = STOPPED
            Twig.debug { "Synchronizer $synchronizerKey stopped" }

            instances.remove(synchronizerKey)
        }
    }

    /**
     * A Flow-providing version of [close] function. This safely closes the Synchronizer together with the related
     * components.
     */
    fun closeFlow(): Flow<Unit> =
        callbackFlow {
            val shutdownJob =
                coroutineScope.launch {
                    Twig.info { "Stopping synchronizer $synchronizerKey…" }
                    processor.stop()
                }

            instances[synchronizerKey] = InstanceState.ShuttingDown(shutdownJob)

            shutdownJob.invokeOnCompletion {
                coroutineScope.cancel()
                _status.value = STOPPED
                Twig.info { "Synchronizer $synchronizerKey stopped" }

                instances.remove(synchronizerKey)

                trySend(Unit)
            }

            awaitClose {
                // Nothing to close here
            }
        }

    override suspend fun rewindToNearestHeight(height: BlockHeight): BlockHeight? {
        return processor.rewindToNearestHeight(height)
    }

    override fun getMemos(transactionOverview: TransactionOverview): Flow<String> {
        return storage.getOutputProperties(transactionOverview.rawId).map { properties ->
            if (!properties.protocol.isShielded()) {
                ""
            } else {
                runCatching {
                    backend.getMemoAsUtf8(
                        txId = transactionOverview.rawId.byteArray,
                        protocol = properties.protocol,
                        outputIndex = properties.index
                    )
                }.onFailure {
                    Twig.error { "Failed to get memo with: $it" }
                }.onSuccess {
                    Twig.debug { "Transaction memo queried: $it" }
                }.fold(
                    onSuccess = { it ?: "" },
                    onFailure = { "" }
                )
            }
        }
    }

    override fun getRecipients(transactionOverview: TransactionOverview): Flow<TransactionRecipient> {
        require(transactionOverview.isSentTransaction) { "Recipients can only be queried for sent transactions" }

        return storage.getRecipients(transactionOverview.rawId)
    }

    override suspend fun getTransactionOutputs(transactionOverview: TransactionOverview): List<TransactionOutput> {
        return storage.getOutputProperties(transactionOverview.rawId).toList().map {
            TransactionOutput(
                when (it.protocol) {
                    ZcashProtocol.TRANSPARENT -> TransactionPool.TRANSPARENT
                    ZcashProtocol.SAPLING -> TransactionPool.SAPLING
                    ZcashProtocol.ORCHARD -> TransactionPool.ORCHARD
                }
            )
        }
    }

    override suspend fun getTransactions(accountUuid: AccountUuid): Flow<List<TransactionOverview>> {
        return combine(
            processor.networkHeight,
            storage.getTransactions(accountUuid)
        ) { networkHeight, allAccountTransactions ->
            val latestBlockHeight =
                networkHeight ?: runCatching {
                    backend.getMaxScannedHeight()
                }.onFailure {
                    Twig.error(it) { "Failed to get max scanned height" }
                }.getOrNull()

            allAccountTransactions.map { TransactionOverview.new(it, latestBlockHeight) }
        }
    }

    //
    // Storage APIs
    //

    // TODO [#682]: turn this section into the data access API. For now, just aggregate all the things that we want
    //  to do with the underlying data
    // TODO [#682]: https://github.com/zcash/zcash-android-wallet-sdk/issues/682

    suspend fun getTransactionCount(): Int {
        return storage.getTransactionCount().toInt()
    }

    fun refreshTransactions() {
        storage.invalidate()
    }

    /**
     * Calculate the latest balance based on the blocks that have been scanned and transmit this information into the
     * [transparentBalance], [saplingBalances], and [orchardBalances] flows.
     */
    suspend fun refreshAllBalances() {
        processor.refreshWalletSummary()
    }

    override suspend fun refreshExchangeRateUsd() {
        coroutineScope.launch {
            refreshExchangeRateUsd.emit(Unit)
        }
    }

    suspend fun isValidAddress(address: String): Boolean {
        return !validateAddress(address).isNotValid
    }

    //
    // Private API
    //

    private fun CoroutineScope.onReady() {
        Twig.debug { "Starting synchronizer…" }

        // Verify processor setup as soon as possible
        launch(CoroutineExceptionHandler(::onCriticalError)) {
            processor.verifySetup()
        }

        // Triggering UTXOs and transactions fetching at the beginning of the block synchronization right after the
        //  app starts makes the transparent transactions appear faster.
        launch(CoroutineExceptionHandler(::onCriticalError)) {
            // Refresh UTXOs and transactions for all the wallet's accounts
            refreshAllAccountsUtxos()
            refreshTransactions()
        }

        launch(CoroutineExceptionHandler(::onCriticalError)) {
            var lastScanTime = 0L
            processor.onProcessorErrorListener = ::onProcessorError
            processor.onSetupErrorListener = ::onSetupError
            processor.onChainErrorListener = ::onChainError

            processor.state.onEach {
                when (it) {
                    is Synced -> {
                        val now = System.currentTimeMillis()
                        // do a bit of housekeeping and then report synced status
                        onScanComplete(it.syncedRange, now - lastScanTime)
                        lastScanTime = now
                        SYNCED
                    }

                    is Stopped -> STOPPED
                    is Disconnected -> DISCONNECTED
                    is Syncing, Initialized -> SYNCING
                }.let { synchronizerStatus ->
                    _status.value = synchronizerStatus
                }
            }.launchIn(this)
            processor.start()
            Twig.debug { "Completed starting synchronizer" }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onCriticalError(
        unused: CoroutineContext?,
        error: Throwable
    ) {
        Twig.error(error) { "Critical error occurred" }

        if (onCriticalErrorHandler == null) {
            Twig.debug {
                "WARNING: a critical error occurred but no callback is registered to be notified " +
                    "of critical errors! THIS IS PROBABLY A MISTAKE. To respond to these " +
                    "errors (perhaps to update the UI or alert the user) set " +
                    "synchronizer.onCriticalErrorHandler to a non-null value."
            }

            onCriticalErrorHandler?.invoke(error)
        }
    }

    private fun onProcessorError(error: Throwable): Boolean {
        Twig.debug { "ERROR while processing data: $error" }
        if (onProcessorErrorHandler == null) {
            Twig.debug {
                "WARNING: falling back to the default behavior for processor errors. To add" +
                    " custom behavior, set synchronizer.onProcessorErrorHandler to" +
                    " a non-null value"
            }
            return true
        }
        return onProcessorErrorHandler?.invoke(error)?.also {
            Twig.debug {
                "processor error handler signaled that we should " +
                    "${if (it) "try again" else "abort"}!"
            }
        } == true
    }

    private fun onSetupError(error: Throwable): Boolean {
        if (onSetupErrorHandler == null) {
            Twig.debug {
                "WARNING: falling back to the default behavior for setup errors. To add custom" +
                    " behavior, set synchronizer.onSetupErrorHandler to a non-null value"
            }
            return false
        }
        return onSetupErrorHandler?.invoke(error) == true
    }

    private fun onChainError(
        errorHeight: BlockHeight,
        rewindHeight: BlockHeight
    ) {
        Twig.debug { "Chain error detected at height: $errorHeight. Rewinding to: $rewindHeight" }
        if (onChainErrorHandler == null) {
            Twig.debug {
                "WARNING: a chain error occurred but no callback is registered to be notified of " +
                    "chain errors. To respond to these errors (perhaps to update the UI or alert the" +
                    " user) set synchronizer.onChainErrorHandler to a non-null value"
            }
        }
        onChainErrorHandler?.invoke(errorHeight, rewindHeight)
    }

    /**
     * @param elapsedMillis the amount of time that passed since the last scan
     */
    private suspend fun onScanComplete(
        scannedRange: ClosedRange<BlockHeight>?,
        elapsedMillis: Long
    ) {
        // We don't need to update anything if there have been no blocks
        // refresh anyway if:
        // - if it's the first time we finished scanning
        // - if we check for blocks 5 times and find nothing was mined
        @Suppress("MagicNumber")
        val shouldRefresh = !scannedRange.isNullOrEmpty() || elapsedMillis > (ZcashSdk.POLL_INTERVAL * 5)
        val reason = if (scannedRange.isNullOrEmpty()) "it's been a while" else "new blocks were scanned"

        // Refresh UTXOs, balances, and transactions for all the wallet's accounts
        if (shouldRefresh) {
            Twig.debug { "Triggering utxo refresh since $reason!" }
            refreshAllAccountsUtxos()

            Twig.debug { "Triggering balance refresh since $reason!" }
            refreshAllBalances()

            Twig.debug { "Triggering transaction refresh since $reason!" }
            refreshTransactions()
        }
    }

    private suspend fun refreshAllAccountsUtxos() {
        getAccounts().forEach { refreshUtxos(it) }
    }

    //
    // Account management
    //

    private val refreshAccountsBus = MutableSharedFlow<Unit>()

    // Not ready to be a public API; internal for testing only
    internal suspend fun createAccount(
        accountName: String,
        keySource: String?,
        recoverUntil: BlockHeight?,
        seed: ByteArray,
        treeState: TreeState,
    ): AccountUsk {
        return runCatching {
            backend.createAccountAndGetSpendingKey(
                accountName = accountName,
                keySource = keySource,
                seed = seed,
                treeState = treeState,
                recoverUntil = recoverUntil
            ).also {
                refreshAccountsBus.emit(Unit)
            }
        }.onFailure {
            Twig.error(it) { "Create account failed." }
        }.getOrElse {
            throw InitializeException.CreateAccountException(it)
        }
    }

    override suspend fun importAccountByUfvk(
        purpose: AccountPurpose,
        setup: AccountImportSetup,
    ): Account {
        val chainTip: BlockHeight? =
            when (val response = processor.downloader.getLatestBlockHeight()) {
                is Response.Success -> {
                    Twig.info { "Chain tip for recovery until param fetched: ${response.result.value}" }
                    runCatching { response.result.toBlockHeight() }.getOrNull()
                }
                is Response.Failure -> {
                    Twig.error {
                        "Chain tip fetch for recovery until failed with: ${response.toThrowable()}"
                    }
                    null
                }
            }

        val loadedCheckpoint =
            CheckpointTool.loadNearest(
                context = context,
                network = network,
                chainTip ?: network.saplingActivationHeight
            )
        val treeState: TreeState = loadedCheckpoint.treeState()

        return runCatching {
            backend.importAccountUfvk(
                purpose = purpose,
                recoverUntil = chainTip,
                setup = setup,
                treeState = treeState,
            ).also {
                refreshAccountsBus.emit(Unit)
            }
        }.onFailure {
            Twig.error(it) { "Import account failed." }
        }.getOrElse {
            throw InitializeException.ImportAccountException(it)
        }
    }

    override suspend fun getAccounts(): List<Account> {
        return runCatching {
            backend.getAccounts()
        }.onFailure {
            Twig.error(it) { "Get wallet accounts failed." }
        }.getOrElse {
            throw InitializeException.GetAccountsException(it)
        }
    }

    override val accountsFlow: Flow<List<Account>?> =
        channelFlow {
            send(getAccounts())
            launch {
                refreshAccountsBus.collect {
                    send(getAccounts())
                }
            }
            awaitClose()
        }.stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(),
            null
        )

    /**
     * Returns the current Unified Address for this account.
     */
    override suspend fun getUnifiedAddress(account: Account): String =
        CompactBlockProcessor.getCurrentAddress(
            backend,
            account
        )

    /**
     * Returns the legacy Sapling address corresponding to the current Unified Address for this account.
     */
    override suspend fun getSaplingAddress(account: Account): String =
        CompactBlockProcessor.getLegacySaplingAddress(
            backend,
            account
        )

    /**
     * Returns the legacy transparent address corresponding to the current Unified Address for this account.
     */
    override suspend fun getTransparentAddress(account: Account): String =
        CompactBlockProcessor.getTransparentAddress(
            backend,
            account
        )

    /**
     * Creates a proposal for fulfilling a payment ZIP-321 URI
     *
     * @param account the account from which to transfer funds.
     * @param uri a ZIP-321 compliant payment URI String
     *
     * @throws TransactionEncoderException.ProposalFromUriException in case the proposal creation failed
     *
     * @return the proposal or an exception
     */
    @Throws(TransactionEncoderException.ProposalFromUriException::class)
    override suspend fun proposeFulfillingPaymentUri(
        account: Account,
        uri: String
    ): Proposal = txManager.proposeTransferFromUri(account, uri)

    /**
     * @throws TransactionEncoderException.ProposalFromParametersException in case the proposal creation failed
     *
     * @return the proposal or an exception
     */
    @Throws(TransactionEncoderException.ProposalFromParametersException::class)
    override suspend fun proposeTransfer(
        account: Account,
        recipient: String,
        amount: Zatoshi,
        memo: String
    ): Proposal = txManager.proposeTransfer(account, recipient, amount, memo)

    /**
     * @throws TransactionEncoderException.ProposalShieldingException in case the proposal creation failed
     *
     * @return the proposal or an exception
     */
    @Throws(TransactionEncoderException.ProposalShieldingException::class)
    override suspend fun proposeShielding(
        account: Account,
        shieldingThreshold: Zatoshi,
        memo: String,
        transparentReceiver: String?
    ): Proposal? = txManager.proposeShielding(account, shieldingThreshold, memo, transparentReceiver)

    @Throws(
        TransactionEncoderException.TransactionNotCreatedException::class,
        TransactionEncoderException.TransactionNotFoundException::class
    )
    override suspend fun createProposedTransactions(
        proposal: Proposal,
        usk: UnifiedSpendingKey
    ): Flow<TransactionSubmitResult> {
        // Internally, this logic submits and checks every incoming transaction, and once [Failure] or
        // [NotAttempted] submission result occurs, it returns [NotAttempted] for the rest of them
        var anySubmissionFailed = false
        return txManager.createProposedTransactions(proposal, usk)
            .asFlow()
            .map { transaction ->
                if (anySubmissionFailed) {
                    TransactionSubmitResult.NotAttempted(transaction.txId)
                } else {
                    val submission = txManager.submit(transaction)
                    when (submission) {
                        is TransactionSubmitResult.Success -> {
                            // Expected state
                        }

                        is TransactionSubmitResult.Failure,
                        is TransactionSubmitResult.NotAttempted -> {
                            anySubmissionFailed = true
                        }
                    }
                    submission
                }
            }
    }

    override suspend fun refreshUtxos(
        account: Account,
        since: BlockHeight
    ): Int {
        return processor.refreshUtxos(account, since)
    }

    override suspend fun getTransparentBalance(tAddr: String): Zatoshi {
        return processor.getUtxoCacheBalance(tAddr)
    }

    override suspend fun isValidShieldedAddr(address: String) = txManager.isValidShieldedAddress(address)

    override suspend fun isValidTransparentAddr(address: String) = txManager.isValidTransparentAddress(address)

    override suspend fun isValidUnifiedAddr(address: String) = txManager.isValidUnifiedAddress(address)

    override suspend fun isValidTexAddr(address: String) = txManager.isValidTexAddress(address)

    override suspend fun validateAddress(address: String): AddressType =
        runCatching {
            if (isValidShieldedAddr(address)) {
                Shielded
            } else if (isValidTransparentAddr(address)) {
                Transparent
            } else if (isValidUnifiedAddr(address)) {
                Unified
            } else if (isValidTexAddr(address)) {
                Tex
            } else {
                AddressType.Invalid("Not a Zcash address")
            }
        }.getOrElse { error ->
            AddressType.Invalid(error.message ?: "Invalid")
        }

    // TODO [#1405]: Fix/Remove broken SdkSynchronizer.validateConsensusBranch function
    // TODO [#1405]: https://github.com/Electric-Coin-Company/zcash-android-wallet-sdk/issues/1405
    override suspend fun validateConsensusBranch(): ConsensusMatchType {
        val serverBranchId = tryNull { processor.downloader.getServerInfo()?.consensusBranchId }

        val currentChainTip =
            when (
                val response =
                    processor.downloader.getLatestBlockHeight()
            ) {
                is Response.Success -> {
                    Twig.info { "Chain tip for validate consensus branch action fetched: ${response.result.value}" }
                    runCatching { response.result.toBlockHeight() }.getOrNull()
                }

                is Response.Failure -> {
                    Twig.error {
                        "Chain tip fetch failed for validate consensus branch action with:" +
                            " ${response.toThrowable()}"
                    }
                    null
                }
            }

        val sdkBranchId =
            currentChainTip?.let {
                tryNull {
                    (txManager as OutboundTransactionManagerImpl).encoder.getConsensusBranchId(currentChainTip)
                }
            }

        return ConsensusMatchType(
            sdkBranchId?.let { ConsensusBranchId.fromId(it) },
            serverBranchId?.let { ConsensusBranchId.fromHex(it) }
        )
    }

    @Suppress("LongMethod", "ReturnCount")
    override suspend fun validateServerEndpoint(
        context: Context,
        endpoint: LightWalletEndpoint
    ): ServerValidation {
        // Create a dedicated light wallet client for the validation
        // The single request timeout is changed from default to 5 seconds to speed up a possible custom server
        // endpoint validation

        LightWalletClient.new(
            context = context,
            lightWalletEndpoint = endpoint,
            singleRequestTimeout = 5.seconds
        ).use { lightWalletClient ->
            val remoteInfo =
                when (val response = lightWalletClient.getServerInfo()) {
                    is Response.Success -> response.result
                    is Response.Failure -> {
                        return ServerValidation.InValid(response.toThrowable())
                    }
                }

            // Check network type
            if (!remoteInfo.matchingNetwork(network.networkName)) {
                return ServerValidation.InValid(
                    CompactBlockProcessorException.MismatchedNetwork(
                        clientNetwork = network.networkName,
                        serverNetwork = remoteInfo.chainName
                    )
                )
            }

            // Check sapling activation height
            runCatching {
                val remoteSaplingActivationHeight = remoteInfo.saplingActivationHeightUnsafe.toBlockHeight()
                if (network.saplingActivationHeight != remoteSaplingActivationHeight) {
                    return ServerValidation.InValid(
                        CompactBlockProcessorException.MismatchedSaplingActivationHeight(
                            clientHeight = network.saplingActivationHeight.value,
                            serverHeight = remoteSaplingActivationHeight.value
                        )
                    )
                }
            }.getOrElse {
                return ServerValidation.InValid(it)
            }

            val currentChainTip =
                when (val response = lightWalletClient.getLatestBlockHeight()) {
                    is Response.Success -> {
                        runCatching { response.result.toBlockHeight() }.getOrElse {
                            return ServerValidation.InValid(it)
                        }
                    }

                    is Response.Failure -> {
                        return ServerValidation.InValid(response.toThrowable())
                    }
                }

            val sdkBranchId =
                runCatching {
                    "%x".format(
                        Locale.ROOT,
                        backend.getBranchIdForHeight(currentChainTip)
                    )
                }.getOrElse {
                    return ServerValidation.InValid(it)
                }

            // Check branch id
            return if (remoteInfo.consensusBranchId.equals(sdkBranchId, true)) {
                ServerValidation.Valid
            } else {
                ServerValidation.InValid(
                    CompactBlockProcessorException.MismatchedConsensusBranch(
                        sdkBranchId,
                        remoteInfo.consensusBranchId
                    )
                )
            }
        }
    }

    @Throws(InitializeException.MissingDatabaseException::class)
    override suspend fun getExistingDataDbFilePath(
        context: Context,
        network: ZcashNetwork,
        alias: String
    ): String {
        return DatabaseCoordinator.getInstance(context).dataDbFile(
            network = network,
            alias = alias
        ).run {
            if (!existsSuspend()) {
                throw InitializeException.MissingDatabaseException(network, alias)
            }
            absolutePath
        }
    }
}

/**
 * Provides a way of constructing a synchronizer where dependencies are injected in.
 *
 * See the helper methods for generating default values.
 */
internal object DefaultSynchronizerFactory {
    internal suspend fun defaultBackend(
        network: ZcashNetwork,
        alias: String,
        saplingParamTool: SaplingParamTool,
        coordinator: DatabaseCoordinator
    ): TypesafeBackend {
        return TypesafeBackendImpl(
            RustBackend.new(
                coordinator.fsBlockDbRoot(network, alias),
                coordinator.dataDbFile(network, alias),
                saplingOutputFile = saplingParamTool.outputParamsFile,
                saplingSpendFile = saplingParamTool.spendParamsFile,
                zcashNetworkId = network.id
            )
        )
    }

    @Suppress("LongParameterList")
    internal suspend fun defaultDerivedDataRepository(
        context: Context,
        databaseFile: File,
        checkpoint: Checkpoint,
        recoverUntil: BlockHeight?,
        rustBackend: TypesafeBackend,
        setup: AccountCreateSetup?,
    ): DerivedDataRepository =
        DbDerivedDataRepository(
            DerivedDataDb.new(
                context = context,
                backend = rustBackend,
                databaseFile = databaseFile,
                checkpoint = checkpoint,
                recoverUntil = recoverUntil,
                setup = setup,
            )
        )

    internal suspend fun defaultCompactBlockRepository(
        blockCacheRoot: File,
        backend: TypesafeBackend
    ): CompactBlockRepository =
        FileCompactBlockRepository.new(
            blockCacheRoot,
            backend
        )

    fun defaultService(
        context: Context,
        lightWalletEndpoint: LightWalletEndpoint
    ): LightWalletClient = LightWalletClient.new(context, lightWalletEndpoint)

    internal fun defaultEncoder(
        backend: TypesafeBackend,
        saplingParamTool: SaplingParamTool,
        repository: DerivedDataRepository
    ): TransactionEncoder = TransactionEncoderImpl(backend, saplingParamTool, repository)

    fun defaultDownloader(
        service: LightWalletClient,
        blockStore: CompactBlockRepository
    ): CompactBlockDownloader = CompactBlockDownloader(service, blockStore)

    internal fun defaultTxManager(
        encoder: TransactionEncoder,
        service: LightWalletClient
    ): OutboundTransactionManager {
        return OutboundTransactionManagerImpl.new(
            encoder,
            service
        )
    }

    internal fun defaultProcessor(
        backend: TypesafeBackend,
        downloader: CompactBlockDownloader,
        repository: DerivedDataRepository,
        birthdayHeight: BlockHeight,
        txManager: OutboundTransactionManager
    ): CompactBlockProcessor =
        CompactBlockProcessor(
            backend = backend,
            downloader = downloader,
            minimumHeight = birthdayHeight,
            repository = repository,
            txManager = txManager,
        )
}

internal data class SynchronizerKey(val zcashNetwork: ZcashNetwork, val alias: String)
