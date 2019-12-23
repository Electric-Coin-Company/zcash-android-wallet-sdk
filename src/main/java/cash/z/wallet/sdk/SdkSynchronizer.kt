package cash.z.wallet.sdk

import android.content.Context
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
 * @param ledger exposes flows of wallet transaction information.
 * @param manager manages and tracks outbound transactions.
 * @param processor saves the downloaded compact blocks to the cache and then scans those blocks for
 * data related to this wallet.
 */
@ExperimentalCoroutinesApi
class SdkSynchronizer internal constructor(
    private val ledger: TransactionRepository,
    private val manager: OutboundTransactionManager,
    val processor: CompactBlockProcessor,
    // TODO: clean this up and don't hold this
    val rustBackend: RustBackend? = null
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
     * inaccurate and outbound transactions should be prevented until this sync is complete.
     */
    override val progress: Flow<Int> = processor.progress


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

    suspend fun refreshBalance() {
        _balances.send(processor.getBalanceInfo())
    }

    private fun CoroutineScope.onReady() = launch(CoroutineExceptionHandler(::onCriticalError)) {
        twig("Synchronizer Ready. Starting processor!")
        processor.onErrorListener = ::onProcessorError
        processor.state.onEach {
            when (it) {
                is Scanned -> {
                    onScanComplete(it.scannedRange)
                    SYNCED
                }
                is Stopped -> STOPPED
                is Disconnected -> DISCONNECTED
                else -> SYNCING
            }.let { synchronizerStatus ->
                _status.send(synchronizerStatus)
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
}

/**
 * Simplest constructor possible. Useful for demos, sample apps or PoC's. Anything more complex
 * will probably want to handle initialization, directly.
 */
fun Synchronizer(
    appContext: Context,
    lightwalletdHost: String = ZcashSdk.DEFAULT_LIGHTWALLETD_HOST,
    lightwalletdPort: Int = ZcashSdk.DEFAULT_LIGHTWALLETD_PORT,
    seed: ByteArray? = null,
    birthday: Initializer.WalletBirthday? = null
): Synchronizer {
    val initializer = Initializer(appContext, lightwalletdHost, lightwalletdPort)
    if (initializer.hasData()) {
        twig("Initializing existing wallet")
        initializer.open()
        twig("${initializer.rustBackend.dbDataPath}")
    } else {
        seed ?: throw IllegalArgumentException(
            "Failed to initialize. A seed is required when no wallet exists on the device."
        )
        if (birthday == null) {
            twig("Initializing new wallet")
            initializer.new(seed, overwrite = true)
        } else {
            twig("Initializing imported wallet")
            initializer.import(seed, birthday, overwrite = true)
        }
    }
    return Synchronizer(appContext, initializer)
}

fun Synchronizer(
    appContext: Context,
    initializer: Initializer
) = Synchronizer(appContext, initializer.rustBackend, initializer.host, initializer.port)

/**
 * Constructor function for building a Synchronizer in the most flexible way possible. This allows
 * a wallet maker to customize any subcomponent of the Synchronzier.
 */
@Suppress("FunctionName")
fun Synchronizer(
    appContext: Context,
    rustBackend: RustBackend,
    lightwalletdHost: String = ZcashSdk.DEFAULT_LIGHTWALLETD_HOST,
    lightwalletdPort: Int = ZcashSdk.DEFAULT_LIGHTWALLETD_PORT,
    ledger: TransactionRepository =
        PagedTransactionRepository(appContext, 10, rustBackend.dbDataPath),
    blockStore: CompactBlockStore = CompactBlockDbStore(appContext, rustBackend.dbCachePath),
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
        processor,
        rustBackend
    )
}
