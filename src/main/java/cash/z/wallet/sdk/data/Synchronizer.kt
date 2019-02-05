package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.data.Synchronizer.SyncState.*
import cash.z.wallet.sdk.exception.SynchronizerException
import cash.z.wallet.sdk.rpc.CompactFormats
import cash.z.wallet.sdk.secure.Wallet
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * The glue. Downloads compact blocks to the database and then scans them for transactions. In order to serve that
 * purpose, this class glues together a variety of key components. Each component contributes to the team effort of
 * providing a simple source of truth to interact with.
 *
 * Another way of thinking about this class is the reference that demonstrates how all the pieces can be tied
 * together.
 */
class Synchronizer(
    val downloader: CompactBlockStream,
    val processor: CompactBlockProcessor,
    val repository: TransactionRepository,
    val activeTransactionManager: ActiveTransactionManager,
    val wallet: Wallet,
    val batchSize: Int = 1000,
    logger: Twig = SilentTwig()
) : Twig by logger {

    //    private val downloader = CompactBlockDownloader("10.0.2.2", 9067)
    private val savedBlockChannel = ConflatedBroadcastChannel<CompactFormats.CompactBlock>()

    private lateinit var blockJob: Job

    private val wasPreviouslyStarted
        get() = savedBlockChannel.isClosedForSend || ::blockJob.isInitialized


    //
    // Public API
    //

    fun activeTransactions() = activeTransactionManager.subscribe()

    fun start(parentScope: CoroutineScope): Synchronizer {
        //  prevent restarts so the behavior of this class is easier to reason about
        if (wasPreviouslyStarted) throw SynchronizerException.FalseStart
        twig("starting")
        blockJob = parentScope.launch {
            continueWithState(determineState())
        }
        return this
    }

    fun stop() {
        twig("stopping")
        blockJob.cancel()
        downloader.stop()
        repository.stop()
        activeTransactionManager.stop()
    }

    suspend fun isOutOfSync(): Boolean = withContext(IO) {
        val latestBlockHeight = downloader.connection.getLatestBlockHeight()
        val ourHeight = processor.cacheDao.latestBlockHeight()
        val tolerance = 10
        val delta = latestBlockHeight - ourHeight
        twig("checking whether out of sync. LatestHeight: $latestBlockHeight  ourHeight: $ourHeight  Delta: $delta   tolerance: $tolerance")
        delta > tolerance
    }

    suspend fun isFirstRun(): Boolean = withContext(IO) {
        // maybe just toggle a flag somewhere rather than inferring based on db status
        !processor.dataDbExists && (!processor.cachDbExists || processor.cacheDao.count() == 0)
    }

    suspend fun sendToAddress(zatoshi: Long, toAddress: String) =
        activeTransactionManager.sendToAddress(zatoshi, toAddress)

    fun cancelSend(transaction: ActiveSendTransaction): Boolean = activeTransactionManager.cancel(transaction)


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
                downloader.start(this, syncState.startingBlockHeight, batchSize)
            launch { monitorProgress(downloader.progress()) }
            activeTransactionManager.start()
            repository.start(this)
            processor.processBlocks(blockChannel)
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
        val state = if (processor.dataDbExists) {
            // this call blocks because it does IO
            val startingBlockHeight = processor.lastProcessedBlock()
            twig("cacheDb exists with last height of $startingBlockHeight")
            if (startingBlockHeight <= 0) FirstRun else ReadyToProcess(startingBlockHeight)
        } else if(processor.cachDbExists) {
            // this call blocks because it does IO
            val startingBlockHeight = processor.lastProcessedBlock()
            twig("cacheDb exists with last height of $startingBlockHeight")
            if (startingBlockHeight <= 0) FirstRun else CacheOnly(startingBlockHeight)
        } else {
            FirstRun
        }

        twig("determined ${state::class.java.simpleName}")
         state
    }

    sealed class SyncState {
        object FirstRun : SyncState()
        class CacheOnly(val startingBlockHeight: Int = Int.MAX_VALUE) : SyncState()
        class ReadyToProcess(val startingBlockHeight: Int = Int.MAX_VALUE) : SyncState()
    }
}