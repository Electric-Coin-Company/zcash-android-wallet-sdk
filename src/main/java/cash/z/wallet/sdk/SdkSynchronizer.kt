package cash.z.wallet.sdk

import android.content.Context
import cash.z.wallet.sdk.Synchronizer.AddressType.Shielded
import cash.z.wallet.sdk.Synchronizer.AddressType.Transparent
import cash.z.wallet.sdk.Synchronizer.Status.*
import cash.z.wallet.sdk.block.CompactBlockDbStore
import cash.z.wallet.sdk.block.CompactBlockDownloader
import cash.z.wallet.sdk.block.CompactBlockProcessor
import cash.z.wallet.sdk.block.CompactBlockProcessor.State.*
import cash.z.wallet.sdk.block.CompactBlockProcessor.WalletBalance
import cash.z.wallet.sdk.block.CompactBlockStore
import cash.z.wallet.sdk.entity.*
import cash.z.wallet.sdk.exception.SynchronizerException
import cash.z.wallet.sdk.ext.ZcashSdk
import cash.z.wallet.sdk.ext.twig
import cash.z.wallet.sdk.ext.twigTask
import cash.z.wallet.sdk.jni.RustBackend
import cash.z.wallet.sdk.service.LightWalletGrpcService
import cash.z.wallet.sdk.service.LightWalletService
import cash.z.wallet.sdk.transaction.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

/**
 * A Synchronizer that attempts to remain operational, despite any number of errors that can occur.
 * It acts as the glue that ties all the pieces of the SDK together. Each component of the SDK is
 * designed for the potential of stand-alone usage but coordinating all the interactions is non-
 * trivial. So the Synchronizer facilitates this, acting as reference that demonstrates how all the
 * pieces can be tied together. Its goal is to allow a developer to focus on their app rather than
 * the nuances of how Zcash works.
 *
 * @property ledger exposes flows of wallet transaction information.
 * @property manager manages and tracks outbound transactions.
 * @property processor saves the downloaded compact blocks to the cache and then scans those blocks for
 * data related to this wallet.
 */
@ExperimentalCoroutinesApi
class SdkSynchronizer internal constructor(
    private val ledger: TransactionRepository,
    private val manager: OutboundTransactionManager,
    val processor: CompactBlockProcessor
) : Synchronizer {
    private val _balances = ConflatedBroadcastChannel(WalletBalance())
    private val _status = ConflatedBroadcastChannel<Synchronizer.Status>(DISCONNECTED)

    /**
     * The lifespan of this Synchronizer. This scope is initialized once the Synchronizer starts
     * because it will be a child of the parentScope that gets passed into the [start] function.
     * Everything launched by this Synchronizer will be cancelled once the Synchronizer or its
     * parentScope stops. This is a lateinit rather than nullable property so that it fails early
     * rather than silently, whenever the scope is used before the Synchronizer has been started.
     */
    lateinit var coroutineScope: CoroutineScope


    //
    // Transactions
    //

    override val balances: Flow<WalletBalance> = _balances.asFlow()
    override val clearedTransactions = ledger.allTransactions
    override val pendingTransactions = manager.getAll()
    override val sentTransactions = ledger.sentTransactions
    override val receivedTransactions = ledger.receivedTransactions


    //
    // Status
    //

    /**
     * Indicates the status of this Synchronizer. This implementation basically simplifies the
     * status of the processor to focus only on the high level states that matter most. Whenever the
     * processor is finished scanning, the synchronizer updates transaction and balance info and
     * then emits a [SYNCED] status.
     */
    override val status = _status.asFlow()

    /**
     * Indicates the download progress of the Synchronizer. When progress reaches 100, that
     * signals that the Synchronizer is in sync with the network. Balances should be considered
     * inaccurate and outbound transactions should be prevented until this sync is complete. It is
     * a simplified version of [processorInfo].
     */
    override val progress: Flow<Int> = processor.progress

    /**
     * Indicates the latest information about the blocks that have been processed by the SDK. This
     * is very helpful for conveying detailed progress and status to the user.
     */
    override val processorInfo: Flow<CompactBlockProcessor.ProcessorInfo> = processor.processorInfo

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
     * A callback to invoke whenever a chain error is encountered. These occur whenever the
     * processor detects a missing or non-chain-sequential block (i.e. a reorg).
     */
    override var onChainErrorHandler: ((Int, Int) -> Any)? = null


    //
    // Public API
    //

    /**
     * Starts this synchronizer within the given scope. For simplicity, attempting to start an
     * instance that has already been started will throw a [SynchronizerException.FalseStart]
     * exception. This reduces the complexity of managing resources that must be recycled. Instead,
     * each synchronizer is designed to have a long lifespan and should be started from an activity,
     * application or session.
     *
     * @param parentScope the scope to use for this synchronizer, typically something with a
     * lifecycle such as an Activity for single-activity apps or a logged in user session. This
     * scope is only used for launching this synchronzer's job as a child. If no scope is provided,
     * then this synchronizer and all of its coroutines will run until stop is called, which is not
     * recommended since it can leak resources. That type of behavior is more useful for tests.
     *
     * @return an instance of this class so that this function can be used fluidly.
     */
    override fun start(parentScope: CoroutineScope?): Synchronizer {
        if (::coroutineScope.isInitialized) throw SynchronizerException.FalseStart

        // base this scope on the parent so that when the parent's job cancels, everything here
        // cancels as well also use a supervisor job so that one failure doesn't bring down the
        // whole synchronizer
        val supervisorJob = SupervisorJob(parentScope?.coroutineContext?.get(Job))
        coroutineScope =
            CoroutineScope(supervisorJob + Dispatchers.Main)
        coroutineScope.onReady()
        return this
    }

    /**
     * Stop this synchronizer and all of its child jobs. Once a synchronizer has been stopped it
     * should not be restarted and attempting to do so will result in an error. Also, this function
     * will throw an exception if the synchronizer was never previously started.
     */
    override fun stop() {
        coroutineScope.launch {
            processor.stop()
            coroutineScope.cancel()
            _balances.cancel()
            _status.cancel()
        }
    }


    //
    // Private API
    //

    private fun refreshTransactions() {
        ledger.invalidate()
    }

    /**
     * Calculate the latest balance, based on the blocks that have been scanned and transmit this
     * information into the flow of [balances].
     */
    suspend fun refreshBalance() {
        twig("refreshing balance")
        _balances.send(processor.getBalanceInfo())
    }

    private fun CoroutineScope.onReady() = launch(CoroutineExceptionHandler(::onCriticalError)) {
        twig("Synchronizer (${this@SdkSynchronizer}) Ready. Starting processor!")
        processor.onProcessorErrorListener = ::onProcessorError
        processor.onChainErrorListener = ::onChainError
        processor.state.onEach {
            when (it) {
                is Scanned -> {
                    // do a bit of housekeeping and then report synced status
                    onScanComplete(it.scannedRange)
                    SYNCED
                }
                is Stopped -> STOPPED
                is Disconnected -> DISCONNECTED
                is Downloading, Initialized -> DOWNLOADING
                is Validating -> VALIDATING
                is Scanning -> SCANNING
                is Enhancing -> ENHANCING
            }.let { synchronizerStatus ->
                //  ignore enhancing status for now
                // TODO: clean this up and handle enhancing gracefully
                if (synchronizerStatus != ENHANCING) _status.send(synchronizerStatus)
            }
        }.launchIn(this)
        processor.start()
        twig("Synchronizer onReady complete. Processor start has exited!")
    }

    private fun onCriticalError(unused: CoroutineContext, error: Throwable) {
        twig("********")
        twig("********  ERROR: $error")
        if (error.cause != null) twig("******** caused by ${error.cause}")
        if (error.cause?.cause != null) twig("******** caused by ${error.cause?.cause}")
        twig("********")

        onCriticalErrorHandler?.invoke(error)
    }

    private fun onFailedSend(error: Throwable): Boolean {
        twig("ERROR while submitting transaction: $error")
        return onSubmissionErrorHandler?.invoke(error)?.also {
            if (it) twig("submission error handler signaled that we should try again!")
        } == true
    }

    private fun onProcessorError(error: Throwable): Boolean {
        twig("ERROR while processing data: $error")
        if (onProcessorErrorHandler == null) {
            twig(
                "WARNING: falling back to the default behavior for processor errors. To add" +
                        " custom behavior, set synchronizer.onProcessorErrorHandler to" +
                        " a non-null value"
            )
            return true
        }
        return onProcessorErrorHandler?.invoke(error)?.also {
            twig(
                "processor error handler signaled that we should " +
                        "${if (it) "try again" else "abort"}!"
            )
        } == true
    }

    private fun onChainError(errorHeight: Int, rewindHeight: Int) {
        twig("Chain error detected at height: $errorHeight. Rewinding to: $rewindHeight")
        if (onChainErrorHandler == null) {
            twig(
                "WARNING: a chain error occurred but no callback is registered to be notified of " +
                "chain errors. To respond to these errors (perhaps to update the UI or alert the" +
                " user) set synchronizer.onChainErrorHandler to a non-null value"
            )
        }
        onChainErrorHandler?.invoke(errorHeight, rewindHeight)
    }

    private suspend fun onScanComplete(scannedRange: IntRange) {
        // TODO: optimize to skip logic here if there are no new transactions with a block height
        //       within the given range

        // TRICKY:
        // Keep an eye on this section because there is a potential for concurrent DB
        // modification. A change in transactions means a change in balance. Calculating the
        // balance requires touching transactions. If both are done in separate threads, the
        // database can have issues. On Android, this would manifest as a false positive for a
        // "malformed database" exception when the database is not actually corrupt but rather
        // locked (i.e. it's a bad error message).
        // The balance refresh is done first because it is coroutine-based and will fully
        // complete by the time the function returns.
        // Ultimately, refreshing the transactions just invalidates views of data that
        // already exists and it completes on another thread so it should come after the
        // balance refresh is complete.
        twigTask("Triggering balance refresh since the processor is synced!") {
            refreshBalance()
        }
        twigTask("Triggering pending transaction refresh!") {
            refreshPendingTransactions()
        }
        twigTask("Triggering transaction refresh since the processor is synced!") {
            refreshTransactions()
        }
    }

    private suspend fun refreshPendingTransactions() {
        // TODO: this would be the place to clear out any stale pending transactions. Remove filter
        //  logic and then delete any pending transaction with sufficient confirmations (all in one
        //  db transaction).
        manager.getAll().first().filter { it.isSubmitSuccess() && !it.isMined() }
            .forEach { pendingTx ->
                twig("checking for updates on pendingTx id: ${pendingTx.id}")
                pendingTx.rawTransactionId?.let { rawId ->
                    ledger.findMinedHeight(rawId)?.let { minedHeight ->
                        twig(
                            "found matching transaction for pending transaction with id" +
                                    " ${pendingTx.id} mined at height ${minedHeight}!"
                        )
                        manager.applyMinedHeight(pendingTx, minedHeight)
                    }
                }
            }
    }


    //
    // Send / Receive
    //

    override suspend fun cancelSpend(transaction: PendingTransaction) = manager.cancel(transaction)

    override suspend fun getAddress(accountId: Int): String = processor.getAddress(accountId)

    override fun sendToAddress(
        spendingKey: String,
        zatoshi: Long,
        toAddress: String,
        memo: String,
        fromAccountIndex: Int
    ): Flow<PendingTransaction> = flow {
        twig("Initializing pending transaction")
        // Emit the placeholder transaction, then switch to monitoring the database
        manager.initSpend(zatoshi, toAddress, memo, fromAccountIndex).let { placeHolderTx ->
            emit(placeHolderTx)
            manager.encode(spendingKey, placeHolderTx).let { encodedTx ->
                if (!encodedTx.isFailedEncoding() && !encodedTx.isCancelled()) {
                    manager.submit(encodedTx)
                }
            }
        }
    }.flatMapLatest {
        twig("Monitoring pending transaction (id: ${it.id}) for updates...")
        manager.monitorById(it.id)
    }.distinctUntilChanged()

    override suspend fun isValidShieldedAddr(address: String) = manager.isValidShieldedAddress(address)

    override suspend fun isValidTransparentAddr(address: String) =
        manager.isValidTransparentAddress(address)

    override suspend fun validateAddress(address: String): Synchronizer.AddressType {
        return try {
            if (isValidShieldedAddr(address)) Shielded else Transparent
        } catch (zError: Throwable) {
            var message = zError.message
            try {
                if (isValidTransparentAddr(address)) Transparent else Shielded
            } catch (tError: Throwable) {
                Synchronizer.AddressType.Invalid(
                    if (message != tError.message) "$message and ${tError.message}" else (message
                        ?: "Invalid")
                )
            }
        }
    }
}

/**
 * A convenience constructor that accepts the information most likely to change and uses defaults
 * for everything else. This is useful for demos, sample apps or PoC's. Anything more complex
 * will probably want to handle initialization, directly.
 *
 * @param appContext the application context. This is mostly used for finding databases and params
 * files within the apps secure storage area.
 * @param lightwalletdHost the lightwalletd host to use for connections.
 * @param lightwalletdPort the lightwalletd port to use for connections.
 * @param seed the seed to use for this wallet, when importing. Null when creating a new wallet.
 * @param birthdayStore the place to store the birthday of this wallet for future reference, which
 * allows something else to manage the state on behalf of the initializer.
 */
@Suppress("FunctionName")
fun Synchronizer(
    appContext: Context,
    lightwalletdHost: String = ZcashSdk.DEFAULT_LIGHTWALLETD_HOST,
    lightwalletdPort: Int = ZcashSdk.DEFAULT_LIGHTWALLETD_PORT,
    seed: ByteArray? = null,
    birthdayStore: Initializer.WalletBirthdayStore = Initializer.DefaultBirthdayStore(appContext)
): Synchronizer {
    val initializer = Initializer(appContext, lightwalletdHost, lightwalletdPort)
    if (seed != null && birthdayStore.hasExistingBirthday()) {
        twig("Initializing existing wallet")
        initializer.open(birthdayStore.getBirthday())
        twig("${initializer.rustBackend.pathDataDb}")
    } else {
        require(seed != null) {
            "Failed to initialize. A seed is required when no wallet exists on the device."
        }
        if (birthdayStore.hasImportedBirthday()) {
            twig("Initializing new wallet")
            initializer.new(seed, birthdayStore.newWalletBirthday, 1, true, true)
        } else {
            twig("Initializing imported wallet")
            initializer.import(seed, birthdayStore.getBirthday(), true, true)
        }
    }
    return Synchronizer(appContext, initializer)
}

/**
 * Constructor function to use in most cases. This is a convenience function for when a wallet has
 * already created an initializer. Meaning, the basic flow is to call either [Initializer.new] or
 * [Initializer.import] on the first run and then [Initializer.open] for all subsequent launches of
 * the wallet. From there, the initializer is passed to this function in order to start syncing from
 * where the wallet left off.
 *
 * @param appContext the application context. This is mostly used for finding databases and params
 * files within the apps secure storage area.
 * @param initializer the helper that is leveraged for creating all the components that the
 * Synchronizer requires. It is mainly responsible for initializing the databases associated with
 * this synchronizer.
 */
@Suppress("FunctionName")
fun Synchronizer(
    appContext: Context,
    initializer: Initializer
): Synchronizer {
    check(initializer.isInitialized) {
        "Error: RustBackend must be loaded before creating the Synchronizer. Verify that either" +
                " the 'open', 'new' or 'import' function has been called on the Initializer."
    }
    return Synchronizer(appContext, initializer.rustBackend, initializer.host, initializer.port)
}

/**
 * Constructor function for building a Synchronizer in the most flexible way possible. This allows
 * a wallet maker to customize any subcomponent of the Synchronzer.
 *
 * @param appContext the application context. This is mostly used for finding databases and params
 * files within the apps secure storage area.
 * @param lightwalletdHost the lightwalletd host to use for connections.
 * @param lightwalletdPort the lightwalletd port to use for connections.
 * @param ledger repository of wallet transactions, providing an agnostic interface to the
 * underlying information.
 * @param blockStore component responsible for storing compact blocks downloaded from lightwalletd.
 * @param service the lightwalletd service that can provide compact blocks and submit transactions.
 * @param encoder the component responsible for encoding transactions.
 * @param downloader the component responsible for downloading ranges of compact blocks.
 * @param manager the component that manages outbound transactions in order to report which ones are
 * still pending, particularly after failed attempts or dropped connectivity. The intent is to help
 * monitor outbound  transactions status through to completion.
 * @param processor the component responsible for processing compact blocks. This is effectively the
 * brains of the synchronizer that implements most of the high-level business logic and determines
 * the current state of the wallet.
 */
@Suppress("FunctionName")
fun Synchronizer(
    appContext: Context,
    rustBackend: RustBackend,
    lightwalletdHost: String = ZcashSdk.DEFAULT_LIGHTWALLETD_HOST,
    lightwalletdPort: Int = ZcashSdk.DEFAULT_LIGHTWALLETD_PORT,
    ledger: TransactionRepository =
        PagedTransactionRepository(appContext, 1000, rustBackend.pathDataDb), // TODO: fix this pagesize bug, small pages should not crash the app. It crashes with: Uncaught Exception: android.view.ViewRootImpl$CalledFromWrongThreadException: Only the original thread that created a view hierarchy can touch its views. and is probably related to FlowPagedList
    blockStore: CompactBlockStore = CompactBlockDbStore(appContext, rustBackend.pathCacheDb),
    service: LightWalletService = LightWalletGrpcService(appContext, lightwalletdHost, lightwalletdPort),
    encoder: TransactionEncoder = WalletTransactionEncoder(rustBackend, ledger),
    downloader: CompactBlockDownloader = CompactBlockDownloader(service, blockStore),
    manager: OutboundTransactionManager =
        PersistentTransactionManager(appContext, encoder, service),
    processor: CompactBlockProcessor =
        CompactBlockProcessor(downloader, ledger, rustBackend, rustBackend.birthdayHeight)
): Synchronizer {
    // ties everything together
    return SdkSynchronizer(
        ledger,
        manager,
        processor
    )
}
