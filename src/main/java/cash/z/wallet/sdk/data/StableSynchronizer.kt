package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.block.CompactBlockProcessor
import cash.z.wallet.sdk.entity.ClearedTransaction
import cash.z.wallet.sdk.entity.PendingTransaction
import cash.z.wallet.sdk.entity.SentTransaction
import cash.z.wallet.sdk.exception.SynchronizerException
import cash.z.wallet.sdk.exception.WalletException
import cash.z.wallet.sdk.secure.Wallet
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlin.coroutines.CoroutineContext

/**
 * A synchronizer that attempts to remain operational, despite any number of errors that can occur. It acts as the glue
 * that ties all the pieces of the SDK together. Each component of the SDK is designed for the potential of stand-alone
 * usage but coordinating all the interactions is non-trivial. So the synchronizer facilitates this, acting as reference
 * that demonstrates how all the pieces can be tied together. Its goal is to allow a developer to focus on their app
 * rather than the nuances of how Zcash works.
 *
 * @param wallet the component that wraps the JNI layer that interacts with the rust backend and manages wallet config.
 * @param repository the component that exposes streams of wallet transaction information.
 * @param sender the component responsible for sending transactions to lightwalletd in order to spend funds.
 * @param processor the component that saves the downloaded compact blocks to the cache and then scans those blocks for
 * data related to this wallet.
 * @param encoder the component that creates a signed transaction, used for spending funds.
 */
@ExperimentalCoroutinesApi
class SdkSynchronizer (
    private val wallet: Wallet,
    private val ledger: TransactionRepository,
    private val sender: TransactionSender,
    private val processor: CompactBlockProcessor,
    private val encoder: TransactionEncoder
) : Synchronizer {

    /**
     * The lifespan of this Synchronizer. This scope is initialized once the Synchronizer starts because it will be a
     * child of the parentScope that gets passed into the [start] function. Everything launched by this Synchronizer
     * will be cancelled once the Synchronizer or its parentScope stops. This is a lateinit rather than nullable
     * property so that it fails early rather than silently, whenever the scope is used before the Synchronizer has been
     * started.
     */
    lateinit var coroutineScope: CoroutineScope


    //
    // Status
    //

    /**
     * A property that is true while a connection to the lightwalletd server exists.
     */
    override val isConnected: Boolean get() = processor.isConnected

    /**
     * A property that is true while actively downloading blocks from lightwalletd.
     */
    override val isSyncing: Boolean get() = processor.isSyncing

    /**
     * A property that is true while actively scanning the cache of compact blocks for transactions.
     */
    override val isScanning: Boolean get() = processor.isScanning


    //
    // Communication Primitives
    //

    /**
     * Channel of balance information.
     */
    private val balanceChannel = ConflatedBroadcastChannel(Wallet.WalletBalance())

    /**
     * Channel of data representing transactions that are pending.
     */
    private val pendingChannel = ConflatedBroadcastChannel<List<PendingTransaction>>(listOf())

    /**
     * Channel of data representing transactions that have been mined.
     */
    private val clearedChannel = ConflatedBroadcastChannel<List<ClearedTransaction>>(listOf())


    //
    // Error Handling
    //

    /**
     * A callback to invoke whenever an uncaught error is encountered. By definition, the return value of the function
     * is ignored because this error is unrecoverable. The only reason the function has a return value is so that all
     * error handlers work with the same signature which allows one function to handle all errors in simple apps. This
     * callback is not called on the main thread so any UI work would need to switch context to the main thread.
     */
    override var onCriticalErrorHandler: ((Throwable?) -> Boolean)? = null

    /**
     * A callback to invoke whenver a processor error is encountered. Returning true signals that the error was handled
     * and a retry attempt should be made, if possible. This callback is not called on the main thread so any UI work
     * would need to switch context to the main thread.
     */
    override var onProcessorErrorHandler: ((Throwable?) -> Boolean)? = null

    /**
     * A callback to invoke whenever a server error is encountered while submitting a transaction to lightwalletd.
     * Returning true signals that the error was handled and a retry attempt should be made, if possible. This callback
     * is not called on the main thread so any UI work would need to switch context to the main thread.
     */
    override var onSubmissionErrorHandler: ((Throwable?) -> Boolean)? = null


    /**
     * Starts this synchronizer within the given scope. For simplicity, attempting to start an instance that has already
     * been started will throw a [SynchronizerException.FalseStart] exception. This reduces the complexity of managing
     * resources that must be recycled. Instead, each synchronizer is designed to have a long lifespan and should be
     * started from an activity, application or session.
     *
     * @param parentScope the scope to use for this synchronizer, typically something with a lifecycle such as an
     * Activity for single-activity apps or a logged in user session. This scope is only used for launching this
     * synchronzer's job as a child.
     */
    override fun start(parentScope: CoroutineScope): Synchronizer {
        if (::coroutineScope.isInitialized) throw SynchronizerException.FalseStart

        // base this scope on the parent so that when the parent's job cancels, everything here cancels as well
        // also use a supervisor job so that one failure doesn't bring down the whole synchronizer
        coroutineScope = CoroutineScope(SupervisorJob(parentScope.coroutineContext[Job]!!) + Dispatchers.Main)

        // TODO: this doesn't work as intended. Refactor to improve the cancellation behavior (i.e. what happens when one job fails) by making launchTransactionMonitor throw an exception
        coroutineScope.launch {
            initWallet()
            startSender(this)

            launchProgressMonitor()
            launchPendingMonitor()
            launchTransactionMonitor()
            onReady()
        }
        return this
    }

    /**
     * Initializes the sender such that it can initiate requests within the scope of this synchronizer.
     */
    private fun startSender(parentScope: CoroutineScope) {
        sender.onSubmissionError = ::onFailedSend
        sender.start(parentScope)
    }

    /**
     * Initialize the wallet, which involves populating data tables based on the latest state of the wallet.
     */
    private suspend fun initWallet() = withContext(IO) {
        try {
            wallet.initialize()
        } catch (e: WalletException.AlreadyInitializedException) {
            twig("Warning: wallet already initialized but this is safe to ignore " +
                    "because the SDK automatically detects where to start downloading.")
        } catch (f: WalletException.FalseStart) {
            if (recoverFrom(f)) {
                twig("Warning: had a wallet init error but we recovered!")
            } else {
                twig("Error: false start while initializing wallet!")
            }
        }
    }

    // TODO: this is a work in progress. We could take drastic measures to automatically recover from certain critical
    //  errors and alert the user but this might be better to do at the app level, rather than SDK level.
    private fun recoverFrom(error: WalletException.FalseStart): Boolean {
        if (error.message?.contains("unable to open database file") == true
            || error.message?.contains("table blocks has no column named") == true) {
            //TODO: these errors are fatal and we need to delete the database and start over
            twig("Database should be deleted and we should start over")
        }
        return false
    }

    /**
     * Stop this synchronizer and all of its child jobs. Once a synchronizer has been stopped it should not be restarted
     * and attempting to do so will result in an error. Also, this function will throw an exception if the synchronizer
     * was never previously started.
     */
    override fun stop() {
        coroutineScope.cancel()
    }


    //
    // Monitors
    //

    // begin the monitor that will update the balance proactively whenever we're done a large scan
    private fun CoroutineScope.launchProgressMonitor(): Job = launch {
        twig("launching progress monitor")
        val progressUpdates = progress()
        for (progress in progressUpdates) {
            if (progress == 100) {
                twig("triggering a balance update because progress is complete")
                refreshBalance()
            }
        }
        twig("done monitoring for progress changes")
    }

    // begin the monitor that will output pending transactions into the pending channel
    private fun CoroutineScope.launchPendingMonitor(): Job = launch {
        twig("launching pending monitor")
        // ask to be notified when the sender notices anything new, while attempting to send
        sender.notifyOnChange(pendingChannel)

        // when those notifications come in, also update the balance
        val channel = pendingChannel.openSubscription()
        for (pending in channel) {
            if(balanceChannel.isClosedForSend) break
            twig("triggering a balance update because pending transactions have changed")
            refreshBalance()
        }
        twig("done monitoring for pending changes and balance changes")
    }

    private fun CoroutineScope.launchTransactionMonitor(): Job = launch {
        ledger.monitorChanges(::onTransactionsChanged)
    }

    fun onTransactionsChanged() {
        coroutineScope.launch {
            twig("triggering a balance update because transactions have changed")
            refreshBalance()
            clearedChannel.send(ledger.getClearedTransactions())
        }
        twig("done handling changed transactions")
    }

    suspend fun refreshBalance() = withContext(IO) {
        if (!balanceChannel.isClosedForSend) {
            balanceChannel.send(wallet.getBalanceInfo())
        } else {
            twig("WARNING: noticed new transactions but the balance channel was closed for send so ignoring!")
        }
    }

    private fun CoroutineScope.onReady() = launch(CoroutineExceptionHandler(::onCriticalError)) {
        twig("Synchronizer Ready. Starting processor!")
        processor.onErrorListener = ::onProcessorError
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
        return onProcessorErrorHandler?.invoke(error)?.also {
            if (it) twig("processor error handler signaled that we should try again!")
        } == true
    }


    //
    // Channels
    //

    override fun balances(): ReceiveChannel<Wallet.WalletBalance> {
        return balanceChannel.openSubscription()
    }

    override fun progress(): ReceiveChannel<Int> = processor.progress()

    override fun pendingTransactions(): ReceiveChannel<List<PendingTransaction>> {
        return pendingChannel.openSubscription()
    }

    override fun clearedTransactions(): ReceiveChannel<List<ClearedTransaction>> {
        return clearedChannel.openSubscription()
    }

    override fun lastPending(): List<PendingTransaction> {
        return if (pendingChannel.isClosedForSend) listOf() else pendingChannel.value
    }

    override fun lastCleared(): List<ClearedTransaction> {
        return if (clearedChannel.isClosedForSend) listOf() else clearedChannel.value
    }

    override fun lastBalance(): Wallet.WalletBalance {
        return balanceChannel.value
    }


    //
    // Send / Receive
    //

    override fun cancelSend(transaction: SentTransaction): Boolean {
        // not implemented
        throw NotImplementedError("Cancellation is not yet implemented " +
                "but should be pretty straight forward, using th PersistentTransactionManager")
    }

    override suspend fun getAddress(accountId: Int): String = withContext(IO) { wallet.getAddress() }

    override suspend fun sendToAddress(
        zatoshi: Long,
        toAddress: String,
        memo: String,
        fromAccountId: Int
    ): PendingTransaction = withContext(IO) {
        sender.sendToAddress(encoder, zatoshi, toAddress, memo, fromAccountId)
    }

}
