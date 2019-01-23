package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.data.Synchronizer.SyncState.FirstRun
import cash.z.wallet.sdk.data.Synchronizer.SyncState.ReadyToProcess
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
    val wallet: Wallet,
    val batchSize: Int = 1000,
    logger: Twig = SilentTwig()
) : Twig by logger {

    //    private val downloader = CompactBlockDownloader("10.0.2.2", 9067)
    private val savedBlockChannel = ConflatedBroadcastChannel<CompactFormats.CompactBlock>()

    private lateinit var blockJob: Job

    private val wasPreviouslyStarted
        get() = savedBlockChannel.isClosedForSend || ::blockJob.isInitialized

    fun blocks(): ReceiveChannel<CompactFormats.CompactBlock> = savedBlockChannel.openSubscription()

    fun start(parentScope: CoroutineScope): Synchronizer {
        //  prevent restarts so the behavior of this class is easier to reason about
        if (wasPreviouslyStarted) throw SynchronizerException.FalseStart
        twig("starting")
        blockJob = parentScope.launch {
            continueWithState(determineState())
        }
        return this
    }

    fun CoroutineScope.continueWithState(syncState: SyncState): Job {
        return when (syncState) {
            FirstRun -> onFirstRun()
            is ReadyToProcess -> onReady(syncState)
        }
    }

    private fun CoroutineScope.onFirstRun(): Job {
        twig("this appears to be a fresh install, beginning first run of application")
        processor.onFirstRun()
        return continueWithState(ReadyToProcess(processor.birthdayHeight))
    }

    private fun CoroutineScope.onReady(syncState: ReadyToProcess) = launch {
        twig("synchronization is ready to begin at height ${syncState.startingBlockHeight}")
        try {
            // TODO: for PIR concerns, introduce some jitter here for where, exactly, the downloader starts
            val blockChannel =
                downloader.start(this, syncState.startingBlockHeight, batchSize)
            repository.start(this)
            processor.processBlocks(blockChannel)
        } finally {
            stop()
        }
    }

    // TODO: get rid of this temporary helper function after syncing with the latest rust code
    suspend fun updateTimeStamp(height: Int): Long? = withContext(IO) {
        val originalBlock = processor.cacheDao.findById(height)
        twig("TMP: found block at height ${height}")
        if (originalBlock != null) {
            val ogBlock = CompactFormats.CompactBlock.parseFrom(originalBlock.data)
            twig("TMP: parsed block! ${ogBlock.height}  ${ogBlock.time}")
            (repository as PollingTransactionRepository).blocks.updateTime(height, ogBlock.time)
            ogBlock.time
        }
        null
    }

    private suspend fun determineState(): SyncState = withContext(IO) {
        twig("determining state (has the app run before, what block did we last see, etc.)")
        val state = if (processor.dataDbExists) {
            // this call blocks because it does IO
            val startingBlockHeight = repository.lastScannedHeight()
            twig("dataDb exists with last height of $startingBlockHeight")
            if (startingBlockHeight == 0L) FirstRun else ReadyToProcess(startingBlockHeight)
        } else {
            FirstRun
        }

        twig("determined ${state::class.java.simpleName}")
         state
    }

    fun stop() {
        twig("stopping")
        blockJob.cancel()
        downloader.stop()
        repository.stop()
    }

    sealed class SyncState {
        object FirstRun : SyncState()
        class ReadyToProcess(val startingBlockHeight: Long = Long.MAX_VALUE) : SyncState()
    }
}