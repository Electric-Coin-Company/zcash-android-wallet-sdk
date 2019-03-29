package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.dao.WalletTransaction
import cash.z.wallet.sdk.data.SdkSynchronizer.SyncState.*
import cash.z.wallet.sdk.exception.SynchronizerException
import cash.z.wallet.sdk.secure.Wallet
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.distinct
import kotlin.coroutines.CoroutineContext

/**
 * The glue. Downloads compact blocks to the database and then scans them for transactions. In order to serve that
 * purpose, this class glues together a variety of key components. Each component contributes to the team effort of
 * providing a simple source of truth to interact with.
 *
 * Another way of thinking about this class is the reference that demonstrates how all the pieces can be tied
 * together.
 */
class SdkSynchronizer(
    private val downloader: CompactBlockStream,
    private val processor: CompactBlockProcessor,
    private val repository: TransactionRepository,
    private val activeTransactionManager: ActiveTransactionManager,
    private val wallet: Wallet,
    private val batchSize: Int = 1000,
    private val blockPollFrequency: Long = CompactBlockStream.DEFAULT_POLL_INTERVAL
) : Synchronizer {

    /**
     * The primary job for this Synchronizer. It leverages structured concurrency to cancel all work when the
     * `parentScope` provided to the [start] method ends.
     */
    private lateinit var blockJob: Job

    /**
     * The state this Synchronizer was in when it started. This is helpful because the conditions that lead to FirstRun
     * or isStale being detected can change quickly so retaining the initial state is useful for walkthroughs or other
     * elements of an app that rely on this information later.
     */
    private lateinit var initialState: SyncState


    //
    // Public API
    //

    /* Lifecycle */

    /**
     * Starts this synchronizer within the given scope. For simplicity, attempting to start an instance that has already
     * been started will throw a [SynchronizerException.FalseStart] exception. This reduces the complexity of managing
     * resources that must be recycled. Instead, each synchronizer is designed to have a long lifespan (similar to act or application) <=- explain usage
     *
     * @param parentScope the scope to use for this synchronizer, typically something with a lifecycle such as an
     * Activity. Implementations should leverage structured concurrency and cancel all jobs when this scope completes.
     */
    override fun start(parentScope: CoroutineScope): Synchronizer {
        val supervisorJob = SupervisorJob(parentScope.coroutineContext[Job])
        //  prevent restarts so the behavior of this class is easier to reason about
        if (wasPreviouslyStarted) throw SynchronizerException.FalseStart
        twig("starting")
        failure = null
        blockJob = parentScope.launch(CoroutineExceptionHandler(exceptionHandler)) {
            supervisorScope {
                continueWithState(determineState())
            }
        }
        return this
    }

    override fun stop() {
        twig("stopping")
        downloader.stop().also { twig("downloader stopped") }
        repository.stop().also { twig("repository stopped") }
        activeTransactionManager.stop().also { twig("activeTransactionManager stopped") }
        Thread.sleep(5000L)
        blockJob.cancel().also { twig("blockJob cancelled") }
        twig("wait for it...")
        Thread.sleep(2000L)
        twig("done stopping")
    }

    /* Channels */

    override fun activeTransactions() = activeTransactionManager.subscribe()
    override fun allTransactions(): ReceiveChannel<List<WalletTransaction>> {
        return repository.allTransactions()
    }
    override fun progress(): ReceiveChannel<Int> {
        return downloader.progress()
    }

    override fun balance(): ReceiveChannel<Wallet.WalletBalance> {
        return wallet.balance()
    }

    /* Status */

    override suspend fun isStale(): Boolean = withContext(IO) {
        val latestBlockHeight = downloader.connection.getLatestBlockHeight()
        val ourHeight = processor.cacheDao.latestBlockHeight()
        val tolerance = 10
        val delta = latestBlockHeight - ourHeight
        twig("checking whether out of sync. LatestHeight: $latestBlockHeight  ourHeight: $ourHeight  Delta: $delta   tolerance: $tolerance")
        delta > tolerance
    }

    override suspend fun isFirstRun(): Boolean = withContext(IO) {
        initialState is FirstRun
    }

    /* Operations */

    override fun getAddress(accountId: Int): String = wallet.getAddress()

    override suspend fun sendToAddress(zatoshi: Long, toAddress: String, memo: String, fromAccountId: Int) =
        activeTransactionManager.sendToAddress(zatoshi, toAddress, memo, fromAccountId)

    override fun cancelSend(transaction: ActiveSendTransaction): Boolean = activeTransactionManager.cancel(transaction)


    //
    // Private API
    //

    private fun CoroutineScope.continueWithState(syncState: SyncState): Job {
        return when (syncState) {
            FirstRun -> onFirstRun()
            is CacheOnly -> onCacheOnly(syncState)
            is ReadyToProcess -> onReady(syncState)
        }
    }

    private fun CoroutineScope.onFirstRun(): Job {
        twig("this appears to be a fresh install, beginning first run of application")
        val firstRunStartHeight = wallet.initialize() // should get the latest sapling tree and return that height
        twig("wallet firstRun returned a value of $firstRunStartHeight")
        return continueWithState(ReadyToProcess(firstRunStartHeight))
    }

    private fun CoroutineScope.onCacheOnly(syncState: CacheOnly): Job {
        twig("we have cached blocks but no data DB, beginning pre-cached version of application")
        val firstRunStartHeight = wallet.initialize(syncState.startingBlockHeight)
        twig("wallet has already cached up to a height of $firstRunStartHeight")
        return continueWithState(ReadyToProcess(firstRunStartHeight))
    }

    private fun CoroutineScope.onReady(syncState: ReadyToProcess) = launch {
        twig("synchronization is ready to begin at height ${syncState.startingBlockHeight}")
        // TODO: for PIR concerns, introduce some jitter here for where, exactly, the downloader starts
        val blockChannel =
            downloader.start(
                this,
                syncState.startingBlockHeight,
                batchSize,
                pollFrequencyMillis = blockPollFrequency
            )
        launch { monitorProgress(downloader.progress()) }
        launch { monitorTransactions(repository.allTransactions().distinct()) }
        activeTransactionManager.start()
        repository.start(this)
        processor.processBlocks(blockChannel)
    }

    private suspend fun monitorProgress(progressChannel: ReceiveChannel<Int>) = withContext(IO) {
        twig("beginning to monitor download progress")
        for (i in progressChannel) {
            if(i >= 100) {
                twig("triggering a proactive scan in a second because all missing blocks have been loaded")
                delay(1000L)
                launch {
                    twig("triggering proactive scan!")
                    processor.scanBlocks()
                    twig("done triggering proactive scan!")
                }
                break
            }
        }
        twig("done monitoring download progress")
    }

    private suspend fun monitorTransactions(transactionChannel: ReceiveChannel<List<WalletTransaction>>) = withContext(IO) {
        twig("beginning to monitor transactions in order to update the balance")
        for (i in transactionChannel) {
            twig("triggering a balance update because transactions have changed")
            wallet.sendBalanceInfo()
            twig("done triggering balance check!")
        }
        twig("done monitoring transactions in order to update the balance")
    }

    //TODO: add state for never scanned . . . where we have some cache but no entries in the data db
    private suspend fun determineState(): SyncState = withContext(IO) {
        twig("determining state (has the app run before, what block did we last see, etc.)")
        initialState = if (processor.dataDbExists) {
            val isInitialized = repository.isInitialized()
            // this call blocks because it does IO
            val startingBlockHeight = Math.max(processor.lastProcessedBlock(), repository.lastScannedHeight())

            twig("cacheDb exists with last height of $startingBlockHeight and isInitialized = $isInitialized")
            if (!repository.isInitialized()) FirstRun else ReadyToProcess(startingBlockHeight)
        } else if(processor.cachDbExists) {
            // this call blocks because it does IO
            val startingBlockHeight = processor.lastProcessedBlock()
            twig("cacheDb exists with last height of $startingBlockHeight")
            if (startingBlockHeight <= 0) FirstRun else CacheOnly(startingBlockHeight)
        } else {
            FirstRun
        }

        twig("determined ${initialState::class.java.simpleName}")
         initialState
    }


    //
    // Error Handling
    //

    private val wasPreviouslyStarted
        get() = ::blockJob.isInitialized

    private var failure: Throwable? = null

    private val exceptionHandler: (c: CoroutineContext, t: Throwable) -> Unit = { _, throwable ->
        twig("********")
        twig("********  ERROR: $throwable")
        if (throwable.cause != null) twig("******** caused by ${throwable.cause}")
        if (throwable.cause?.cause != null) twig("******** caused by ${throwable.cause?.cause}")
        twig("********")

        val hasRecovered = onSynchronizerErrorListener?.invoke(throwable)
        if (hasRecovered != true) stop().also { failure = throwable }
    }

    override var onSynchronizerErrorListener: ((Throwable?) -> Boolean)? = null
        set(value) {
            field = value
            if (failure != null) value?.invoke(failure)
        }


    sealed class SyncState {
        object FirstRun : SyncState()
        class CacheOnly(val startingBlockHeight: Int = Int.MAX_VALUE) : SyncState()
        class ReadyToProcess(val startingBlockHeight: Int = Int.MAX_VALUE) : SyncState()
    }

}