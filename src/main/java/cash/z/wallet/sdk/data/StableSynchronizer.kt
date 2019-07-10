package cash.z.wallet.sdk.data

import cash.z.android.wallet.data.ChannelListValueProvider
import cash.z.wallet.sdk.block.CompactBlockProcessor
import cash.z.wallet.sdk.dao.WalletTransaction
import cash.z.wallet.sdk.db.PendingTransactionEntity
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
    private val encoder: RawTransactionEncoder,
    private val clearedTransactionProvider: ChannelListValueProvider<WalletTransaction>
) : DataSyncronizer {

    /** This listener will not be called on the main thread. So it will need to switch to do anything with UI, like dialogs */
    override var onCriticalErrorListener: ((Throwable) -> Boolean)? = null

    private var syncJob: Job? = null
    private var clearedJob: Job? = null
    private var pendingJob: Job? = null
    private var progressJob: Job? = null

    private val balanceChannel = ConflatedBroadcastChannel(Wallet.WalletBalance())
    private val progressChannel = ConflatedBroadcastChannel(0)
    private val pendingChannel = ConflatedBroadcastChannel<List<PendingTransactionEntity>>(listOf())
    private val clearedChannel = clearedTransactionProvider.channel

    // TODO: clean these up and turn them into delegates
    internal val pendingProvider = ChannelListValueProvider(pendingChannel)

    override val isConnected: Boolean get() = processor.isConnected
    override val isSyncing: Boolean get() = processor.isSyncing
    override val isScanning: Boolean get() = processor.isScanning

    // TODO: find a better way to expose the lifecycle of this synchronizer (right now this is only used by the zcon1 app's SendReceiver)
    lateinit var internalScope: CoroutineScope
    override fun start(scope: CoroutineScope) {
        internalScope = scope
        twig("Starting sender!")
        try {
            wallet.initialize()
        } catch (e: WalletException.AlreadyInitializedException) {
            twig("Warning: wallet already initialized but this is safe to ignore " +
                    "because the SDK now automatically detects where to start downloading.")
        } catch (f: WalletException.FalseStart) {
            if (recoverFrom(f)) {
                twig("Warning: had a wallet init error but we recovered!")
            } else {
                twig("Error: false start while initializing wallet!")
            }
        }
        sender.onSubmissionError = ::onFailedSend
        sender.start(scope)
        progressJob = scope.launchProgressMonitor()
        pendingJob = scope.launchPendingMonitor()
        clearedJob = scope.launchClearedMonitor()
        syncJob = scope.onReady()
    }

    private fun recoverFrom(error: WalletException.FalseStart): Boolean {
        if (error.message?.contains("unable to open database file") == true
            || error.message?.contains("table blocks has no column named") == true) {
            //TODO: these errors are fatal and we need to delete the database and start over
            twig("Database should be deleted and we should start over")
        }
        return true
    }

    // TODO: consider removing the need for stopping by wrapping everything in a job that gets cancelled
    // probably just returning the job from start
    override fun stop() {
        sender.stop()
        // TODO: consider wrapping these in another object that helps with cleanup like job.toScopedJob()
        // it would keep a reference to the job and then clear that reference when the scope ends
        syncJob?.cancel().also { syncJob = null }
        pendingJob?.cancel().also { pendingJob = null }
        clearedJob?.cancel().also { clearedJob = null }
        progressJob?.cancel().also { progressJob = null }
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

    override fun pendingTransactions(): ReceiveChannel<List<PendingTransactionEntity>> {
        return pendingChannel.openSubscription()
    }

    override fun clearedTransactions(): ReceiveChannel<List<WalletTransaction>> {
        return clearedChannel.openSubscription()
    }

    override fun getPending(): List<PendingTransactionEntity> {
        return pendingProvider.getLatestValue()
    }

    override fun getCleared(): List<WalletTransaction> {
        return clearedTransactionProvider.getLatestValue()
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
    ): PendingTransactionEntity = withContext(IO) {
        sender.sendToAddress(encoder, zatoshi, toAddress, memo, fromAccountId)
    }

}


interface DataSyncronizer : ClearedTransactionProvider, PendingTransactionProvider {
    fun start(scope: CoroutineScope)
    fun stop()

    suspend fun getAddress(accountId: Int = 0): String
    suspend fun sendToAddress(zatoshi: Long, toAddress: String, memo: String = "", fromAccountId: Int = 0): PendingTransactionEntity

    fun balances(): ReceiveChannel<Wallet.WalletBalance>
    fun progress(): ReceiveChannel<Int>
    fun pendingTransactions(): ReceiveChannel<List<PendingTransactionEntity>>
    fun clearedTransactions(): ReceiveChannel<List<WalletTransaction>>

    val isConnected: Boolean
    val isSyncing: Boolean
    val isScanning: Boolean
    var onCriticalErrorListener: ((Throwable) -> Boolean)?
    override fun getPending(): List<PendingTransactionEntity>
    override fun getCleared(): List<WalletTransaction>
    fun getBalance(): Wallet.WalletBalance
}

interface ClearedTransactionProvider {
    fun getCleared(): List<WalletTransaction>
}

interface PendingTransactionProvider {
    fun getPending(): List<PendingTransactionEntity>
}
