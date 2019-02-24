package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.dao.WalletTransaction
import cash.z.wallet.sdk.data.SdkSynchronizer.SyncState.*
import cash.z.wallet.sdk.exception.SynchronizerException
import cash.z.wallet.sdk.secure.Wallet
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.ReceiveChannel

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

    private lateinit var blockJob: Job
    private lateinit var initialState: SyncState

    private val wasPreviouslyStarted
        get() = ::blockJob.isInitialized


    //
    // Public API
    //

    /* Lifecycle */

    override fun start(parentScope: CoroutineScope): Synchronizer {
        //  prevent restarts so the behavior of this class is easier to reason about
        if (wasPreviouslyStarted) throw SynchronizerException.FalseStart
        twig("starting")
        blockJob = parentScope.launch {
            continueWithState(determineState())
        }
        return this
    }

    override fun stop() {
        twig("stopping")
        blockJob.cancel()
        downloader.stop()
        repository.stop()
        activeTransactionManager.stop()
    }

    /* Channels */

    override fun activeTransactions() = activeTransactionManager.subscribe()
    override fun allTransactions(): ReceiveChannel<List<WalletTransaction>> {
        return repository.allTransactions()
    }
    override fun progress(): ReceiveChannel<Int> {
        return downloader.progress()
    }

    override fun balance(): ReceiveChannel<Long> {
        return repository.balance()
    }

    /* Status */

    override suspend fun isOutOfSync(): Boolean = withContext(IO) {
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

    override fun getAddress() = wallet.getAddress()

    override suspend fun sendToAddress(zatoshi: Long, toAddress: String) =
        activeTransactionManager.sendToAddress(zatoshi, toAddress)

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
        try {
            // TODO: for PIR concerns, introduce some jitter here for where, exactly, the downloader starts
            val blockChannel =
                downloader.start(
                    this,
                    syncState.startingBlockHeight,
                    batchSize,
                    pollFrequencyMillis = blockPollFrequency
                )
            launch { monitorProgress(downloader.progress()) }
            activeTransactionManager.start()
            repository.start(this)
            processor.processBlocks(blockChannel)
        } catch(t:Throwable) {
            // TODO: find the best mechanism for error handling
            twig("catching an error $t caused by ${t.cause} <and> ${t.cause?.cause} <and> ${t.cause?.cause?.cause} ")
        } finally {
            stop()
        }
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

    sealed class SyncState {
        object FirstRun : SyncState()
        class CacheOnly(val startingBlockHeight: Int = Int.MAX_VALUE) : SyncState()
        class ReadyToProcess(val startingBlockHeight: Int = Int.MAX_VALUE) : SyncState()
    }
}