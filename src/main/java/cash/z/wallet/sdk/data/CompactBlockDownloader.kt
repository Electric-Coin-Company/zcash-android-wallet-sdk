package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.ext.debug
import cash.z.wallet.sdk.ext.toBlockRange
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import rpc.CompactFormats.CompactBlock
import rpc.CompactTxStreamerGrpc
import rpc.CompactTxStreamerGrpc.CompactTxStreamerBlockingStub
import rpc.Service
import java.io.Closeable

/**
 * Serves as a source of compact blocks received from the light wallet server. Once started, it will
 * request all the appropriate blocks and then stream them into the channel returned when calling [start].
 */
class CompactBlockDownloader private constructor() {
    private lateinit var connection: Connection

    constructor(host: String, port: Int) : this() {
        // TODO: improve the creation of this channel (tweak its settings to use mobile device responsibly) and make sure it is properly cleaned up
        val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
        connection = Connection(CompactTxStreamerGrpc.newBlockingStub(channel))
    }

    constructor(connection: Connection) : this() {
        this.connection = connection
    }

    fun start(
        scope: CoroutineScope,
        startingBlockHeight: Long = Long.MAX_VALUE,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        pollFrequencyMillis: Long = DEFAULT_POLL_INTERVAL
    ): ReceiveChannel<CompactBlock> {
        if(connection.isClosed()) throw IllegalStateException("Cannot start downloader when connection is closed.")
        scope.launch {
            delay(1000L)
            connection.use {
                var latestBlockHeight = it.getLatestBlockHeight()
                if (startingBlockHeight < latestBlockHeight) {
                    latestBlockHeight = it.downloadMissingBlocks(startingBlockHeight, batchSize)
                }
                it.streamBlocks(pollFrequencyMillis, latestBlockHeight)
            }
        }

        return connection.subscribe()
    }

    fun stop() {
        connection.close()
    }

    companion object {
        const val DEFAULT_BATCH_SIZE = 10_000
        const val DEFAULT_POLL_INTERVAL = 75_000L
    }
    
    class Connection(private val blockingStub: CompactTxStreamerBlockingStub): Closeable {
        private var job: Job? = null
        private var syncJob: Job? = null
        private val compactBlockChannel = BroadcastChannel<CompactBlock>(100)

        fun subscribe() = compactBlockChannel.openSubscription()

        /**
         * Download all the missing blocks and return the height of the last block downloaded, which can be used to
         * calculate the total number of blocks downloaded.
         */
        suspend fun downloadMissingBlocks(startingBlockHeight: Long, batchSize: Int = DEFAULT_BATCH_SIZE): Long {
            debug("[Downloader:${System.currentTimeMillis()}] downloadingMissingBlocks starting at $startingBlockHeight")
            val latestBlockHeight = getLatestBlockHeight()
            var downloadedBlockHeight = startingBlockHeight
            // if blocks are missing then download them
            if (startingBlockHeight < latestBlockHeight) {
                val missingBlockCount = latestBlockHeight - startingBlockHeight + 1
                val batches = missingBlockCount / batchSize + (if (missingBlockCount.rem(batchSize) == 0L) 0 else 1)
                debug("[Downloader:${System.currentTimeMillis()}] found $missingBlockCount missing blocks, downloading in $batches batches...")
                for (i in 1..batches) {
                    val end = Math.min(startingBlockHeight + (i * batchSize), latestBlockHeight + 1)
                    loadBlockRange(downloadedBlockHeight..(end-1))
                    downloadedBlockHeight = end
                }
            } else {
                debug("[Downloader:${System.currentTimeMillis()}] no missing blocks to download!")
            }
            return downloadedBlockHeight
        }

        suspend fun getLatestBlockHeight(): Long = withContext(IO) {
            blockingStub.getLatestBlock(Service.ChainSpec.newBuilder().build()).height
        }

        suspend fun streamBlocks(pollFrequencyMillis: Long = DEFAULT_POLL_INTERVAL, startingBlockHeight: Long = Long.MAX_VALUE) = withContext(IO) {
            debug("[Downloader:${System.currentTimeMillis()}] streamBlocks started at $startingBlockHeight with interval $pollFrequencyMillis")
            // start with the next block, unless we were asked to start before then
            var nextBlockHeight = Math.min(startingBlockHeight, getLatestBlockHeight() + 1)
            while (isActive && !compactBlockChannel.isClosedForSend) {
                debug("[Downloader:${System.currentTimeMillis()}] polling on thread ${Thread.currentThread().name} . . .")
                val latestBlockHeight = getLatestBlockHeight()
                if (latestBlockHeight >= nextBlockHeight) {
                    debug("[Downloader:${System.currentTimeMillis()}] found a new block! (latest: $latestBlockHeight) on thread ${Thread.currentThread().name}")
                    loadBlockRange(nextBlockHeight..latestBlockHeight)
                    nextBlockHeight = latestBlockHeight + 1
                } else {
                    debug("[Downloader:${System.currentTimeMillis()}] no new block yet (latest: $latestBlockHeight) on thread ${Thread.currentThread().name}")
                }
                delay(pollFrequencyMillis)
            }
        }

        suspend fun loadBlockRange(range: LongRange): Int = withContext(IO) {
            debug("[Downloader:${System.currentTimeMillis()}] requesting block range $range on thread ${Thread.currentThread().name}")
            val result = blockingStub.getBlockRange(range.toBlockRange())
            var resultCount = 0
            while (result.hasNext()) { //hasNext blocks
                resultCount++
                val nextBlock = result.next()
                debug("[Downloader:${System.currentTimeMillis()}] received new block: ${nextBlock.height} on thread ${Thread.currentThread().name}")
                compactBlockChannel.send(nextBlock)
            }
            resultCount
        }

        fun isClosed(): Boolean {
            return compactBlockChannel.isClosedForSend
        }

        override fun close() {
            compactBlockChannel.cancel()
            syncJob?.cancel()
            syncJob = null
            job?.cancel()
            job = null
        }
    }
}