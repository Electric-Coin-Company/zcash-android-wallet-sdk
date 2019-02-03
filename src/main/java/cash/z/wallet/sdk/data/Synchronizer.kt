package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.data.Synchronizer.SyncState.FirstRun
import cash.z.wallet.sdk.data.Synchronizer.SyncState.ReadyToProcess
import cash.z.wallet.sdk.exception.SynchronizerException
import cash.z.wallet.sdk.ext.masked
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
    }

    suspend fun isFirstRun(): Boolean = withContext(IO) {
        !processor.dataDbExists || processor.cacheDao.count() == 0
    }

    // TODO: pull all these twigs into the activeTransactionManager
    suspend fun sendToAddress(zatoshi: Long, toAddress: String) = withContext(IO) { // don't expose accounts yet
        val activeSendTransaction = activeTransactionManager.create(zatoshi, toAddress.masked())
        val transactionId: Long = wallet.sendToAddress(zatoshi, toAddress)
        if (transactionId < 0) {
            activeTransactionManager.failure(activeSendTransaction, "Failed to create, possibly due to insufficient funds or an invalid key")
            return@withContext
        }
        val transactionRaw: ByteArray? = repository.findTransactionById(transactionId)?.raw
        if (transactionRaw == null) {
            activeTransactionManager.failure(activeSendTransaction, "Failed to find the transaction that we just attempted to create in the dataDb")
            return@withContext
        }

        activeTransactionManager.created(activeSendTransaction, transactionId)
        try {
            twig("attempting to submit transaction $transactionId")
            activeTransactionManager.upload(activeSendTransaction)
            downloader.connection.submitTransaction(transactionRaw)
            activeTransactionManager.awaitConfirmation(activeSendTransaction)
            twig("successfully submitted")
        } catch (t: Throwable) {
            twig("submit failed due to $t")
            var revertMessage = "failed to submit transaction and failed to revert pending send id $transactionId in the dataDb."
            try {
                repository.deleteTransactionById(transactionId)
                revertMessage = "failed to submit transaction. The pending send with id $transactionId has been removed from the DB."
            } catch (t: Throwable) {
            } finally {
                activeTransactionManager.failure(activeSendTransaction, "$revertMessage Failure caused by: ${t.message}")
            }
        }
    }

//    fun blocks(): ReceiveChannel<CompactFormats.CompactBlock> = savedBlockChannel.openSubscription()


    //
    // Private API
    //

    private fun CoroutineScope.continueWithState(syncState: SyncState): Job {
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
            launch { monitorProgress(downloader.progress()) }
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
                twig("triggering proactive scan!")
                launch { processor.scanBlocks() }
                break
            }
        }
        twig("done monitoring download progress")
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

    sealed class SyncState {
        object FirstRun : SyncState()
        class ReadyToProcess(val startingBlockHeight: Long = Long.MAX_VALUE) : SyncState()
    }
}