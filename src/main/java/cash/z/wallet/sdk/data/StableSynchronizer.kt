package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.block.CompactBlockProcessor
import cash.z.wallet.sdk.dao.ClearedTransaction
import cash.z.wallet.sdk.db.PendingTransaction
import cash.z.wallet.sdk.exception.WalletException
import cash.z.wallet.sdk.secure.Wallet
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlin.coroutines.CoroutineContext

/**
 * A synchronizer that attempts to remain operational, despite any number of errors that can occur.
 */
@ExperimentalCoroutinesApi
class StableSynchronizer (
    private val wallet: Wallet,
    private val ledger: PollingTransactionRepository,
    private val sender: TransactionSender,
    private val processor: CompactBlockProcessor,
    private val encoder: RawTransactionEncoder
) : DataSynchronizer {

    /**
     * The lifespan of this Synchronizer. This scope is initialized once the Synchronizer starts because it will be a
     * child of the parentScope that gets passed into the [start] function. Everything launched by this Synchronizer
     * will be cancelled once the Synchronizer or its parentScope stops. This is a lateinit rather than nullable
     * property so that it fails early rather than silently, whenever the scope is used before the Synchronizer has been
     * started.
     */
    lateinit var coroutineScope: CoroutineScope


    //
    // Communication Primitives
    //

    private val balanceChannel = ConflatedBroadcastChannel(Wallet.WalletBalance())
    private val progressChannel = ConflatedBroadcastChannel(0)
    private val pendingChannel = ConflatedBroadcastChannel<List<PendingTransaction>>(listOf())
    private val clearedChannel = ConflatedBroadcastChannel<List<ClearedTransaction>>(listOf())


    //
    // Status
    //

    override val isConnected: Boolean get() = processor.isConnected
    override val isSyncing: Boolean get() = processor.isSyncing
    override val isScanning: Boolean get() = processor.isScanning


    //
    // Error Callbacks
    //

    /** This listener will not be called on the main thread. So it will need to switch to do anything with UI, like dialogs */
    override var onCriticalErrorListener: ((Throwable) -> Boolean)? = null


    override fun start(parentScope: CoroutineScope) {
        // base this scope on the parent so that when the parent's job cancels, everything here cancels as well
        // also use a supervisor job so that one failure doesn't bring down the whole synchronizer
        coroutineScope = CoroutineScope(SupervisorJob(parentScope.coroutineContext[Job]!!) + Dispatchers.Main)

        coroutineScope.launch {
            initWallet()
            startSender(this)

            launchProgressMonitor()
            launchPendingMonitor()
            launchClearedMonitor()
            onReady()
        }
    }

    private fun startSender(parentScope: CoroutineScope) {
        sender.onSubmissionError = ::onFailedSend
        sender.start(parentScope)
    }

    private suspend fun initWallet() = withContext(Dispatchers.IO) {
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

    private fun recoverFrom(error: WalletException.FalseStart): Boolean {
        if (error.message?.contains("unable to open database file") == true
            || error.message?.contains("table blocks has no column named") == true) {
            //TODO: these errors are fatal and we need to delete the database and start over
            twig("Database should be deleted and we should start over")
        }
        return false
    }

    override fun stop() {
        coroutineScope.cancel()
    }


    //
    // Monitors
    //

    // begin the monitor that will update the balance proactively whenever we're done a large scan
    private fun CoroutineScope.launchProgressMonitor(): Job? = launch {
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
    private fun CoroutineScope.launchPendingMonitor(): Job? = launch {
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

    // begin the monitor that will output cleared transactions into the cleared channel
    private fun CoroutineScope.launchClearedMonitor(): Job? = launch {
        twig("launching cleared monitor")
        // poll for modifications and send them into the cleared channel
        ledger.poll(clearedChannel, 10_000L)

        // when those notifications come in, also update the balance
        val channel = clearedChannel.openSubscription()
        for (cleared in channel) {
            if(!balanceChannel.isClosedForSend) {
                twig("triggering a balance update because cleared transactions have changed")
                refreshBalance()
            } else {
                twig("WARNING: noticed new cleared transactions but the balance channel was closed for send so ignoring!")
            }
        }
        twig("done monitoring for cleared changes and balance changes")
    }

    suspend fun refreshBalance() = withContext(IO) {
        balanceChannel.send(wallet.getBalanceInfo())
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


        onCriticalErrorListener?.invoke(error)
    }

    var sameErrorCount = 1
    var processorErrorMessage: String? = ""
    private fun onProcessorError(error: Throwable): Boolean {
        val dummyContext = CoroutineName("bob")
        if (processorErrorMessage == error.message) sameErrorCount++
        val isFrequent = sameErrorCount.rem(25) == 0
        when {
            sameErrorCount == 5 -> onCriticalError(dummyContext, error)
//            isFrequent -> trackError(ProcessorRepeatedFailure(error, sameErrorCount))
            sameErrorCount == 120 -> {
//                trackError(ProcessorMaxFailureReached(error))
                Thread.sleep(500)
                throw error
            }
        }


        processorErrorMessage = error.message
        twig("synchronizer sees your error and ignores it, willfully! Keep retrying ($sameErrorCount), processor!")
        return true
    }

    fun onFailedSend(throwable: Throwable) {
//        trackError(ErrorSubmitting(throwable))
    }


    //
    // Channels
    //

    override fun balances(): ReceiveChannel<Wallet.WalletBalance> {
        return balanceChannel.openSubscription()
    }

    override fun progress(): ReceiveChannel<Int> {
        return progressChannel.openSubscription()
    }

    override fun pendingTransactions(): ReceiveChannel<List<PendingTransaction>> {
        return pendingChannel.openSubscription()
    }

    override fun clearedTransactions(): ReceiveChannel<List<ClearedTransaction>> {
        return clearedChannel.openSubscription()
    }

    override fun getPending(): List<PendingTransaction> {
        return if (pendingChannel.isClosedForSend) listOf() else pendingChannel.value
    }

    override fun getCleared(): List<ClearedTransaction> {
        return if (clearedChannel.isClosedForSend) listOf() else clearedChannel.value
    }

    override fun getBalance(): Wallet.WalletBalance {
        return balanceChannel.value
    }


    //
    // Send / Receive
    //

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


interface DataSynchronizer {
    fun start(parentScope: CoroutineScope)
    fun stop()

    suspend fun getAddress(accountId: Int = 0): String
    suspend fun sendToAddress(zatoshi: Long, toAddress: String, memo: String = "", fromAccountId: Int = 0): PendingTransaction

    fun balances(): ReceiveChannel<Wallet.WalletBalance>
    fun progress(): ReceiveChannel<Int>
    fun pendingTransactions(): ReceiveChannel<List<PendingTransaction>>
    fun clearedTransactions(): ReceiveChannel<List<ClearedTransaction>>

    fun getPending(): List<PendingTransaction>
    fun getCleared(): List<ClearedTransaction>
    fun getBalance(): Wallet.WalletBalance

    val isConnected: Boolean
    val isSyncing: Boolean
    val isScanning: Boolean
    var onCriticalErrorListener: ((Throwable) -> Boolean)?

}
