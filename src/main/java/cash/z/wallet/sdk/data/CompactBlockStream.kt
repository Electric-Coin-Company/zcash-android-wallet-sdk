package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.exception.CompactBlockStreamException
import cash.z.wallet.sdk.ext.toBlockRange
import cash.z.wallet.sdk.rpc.CompactFormats.CompactBlock
import cash.z.wallet.sdk.rpc.CompactTxStreamerGrpc
import cash.z.wallet.sdk.rpc.CompactTxStreamerGrpc.CompactTxStreamerBlockingStub
import cash.z.wallet.sdk.rpc.Service
import com.google.protobuf.ByteString
import io.grpc.Channel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.distinct
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * Serves as a source of compact blocks received from the light wallet server. Once started, it will
 * request all the appropriate blocks and then stream them into the channel returned when calling [start].
 */
class CompactBlockStream private constructor(logger: Twig = SilentTwig()) : Twig by logger {
    lateinit var connection: Connection

    // TODO: improve the creation of this channel (tweak its settings to use mobile device responsibly) and make sure it is properly cleaned up
    constructor(host: String, port: Int, logger: Twig = SilentTwig()) : this(
        ManagedChannelBuilder.forAddress(host, port).usePlaintext().build(), logger
    )

    constructor(channel: Channel, logger: Twig = SilentTwig()) : this(logger) {
        connection = Connection(channel)
    }

    fun start(
        scope: CoroutineScope,
        startingBlockHeight: Long = Long.MAX_VALUE,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        pollFrequencyMillis: Long = DEFAULT_POLL_INTERVAL
    ): ReceiveChannel<CompactBlock> {
        if(connection.isClosed()) throw CompactBlockStreamException.ConnectionClosed
        twig("starting")
        scope.launch {
            twig("preparing to stream blocks...")
            delay(1000L) // TODO: we can probably get rid of this delay.
            try {
                connection.use {
                    twig("requesting latest block height")
                    var latestBlockHeight = it.getLatestBlockHeight()
                    twig("responded with latest block height of $latestBlockHeight")
                    if (startingBlockHeight < latestBlockHeight) {
                        twig("downloading missing blocks from $startingBlockHeight to $latestBlockHeight")
                        latestBlockHeight = it.downloadMissingBlocks(startingBlockHeight, batchSize)
                        twig("done downloading missing blocks")
                    }
                    it.streamBlocks(pollFrequencyMillis, latestBlockHeight)
                }
            } finally {
                stop()
            }
        }

        return connection.subscribe()
    }

    fun progress() = connection.progress().distinct()

    fun stop() {
        twig("stopping")
        connection.close()
    }

    companion object {
        const val DEFAULT_BATCH_SIZE = 10_000
        const val DEFAULT_POLL_INTERVAL = 75_000L
        const val DEFAULT_RETRIES = 5
    }
    
    inner class Connection(private val channel: Channel): Closeable {
        private var job: Job? = null
        private var syncJob: Job? = null
        private val compactBlockChannel = BroadcastChannel<CompactBlock>(100)
        private val progressChannel = BroadcastChannel<Int>(100)

        fun createStub(timeoutMillis: Long = 60_000L): CompactTxStreamerBlockingStub {
            return CompactTxStreamerGrpc.newBlockingStub(channel).withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS)
        }

        fun subscribe() = compactBlockChannel.openSubscription()

        fun progress() = progressChannel.openSubscription()

        /**
         * Download all the missing blocks and return the height of the last block downloaded, which can be used to
         * calculate the total number of blocks downloaded.
         */
        suspend fun downloadMissingBlocks(startingBlockHeight: Long, batchSize: Int = DEFAULT_BATCH_SIZE): Long {
            twig("downloadingMissingBlocks starting at $startingBlockHeight")
            val latestBlockHeight = getLatestBlockHeight()
            var downloadedBlockHeight = startingBlockHeight
            // if blocks are missing then download them
            if (startingBlockHeight < latestBlockHeight) {
                val missingBlockCount = latestBlockHeight - startingBlockHeight + 1
                val batches = missingBlockCount / batchSize + (if (missingBlockCount.rem(batchSize) == 0L) 0 else 1)
                var progress: Int
                twig("found $missingBlockCount missing blocks, downloading in $batches batches...")
                for (i in 1..batches) {
                    retryUpTo(DEFAULT_RETRIES) {
                        twig("beginning batch $i")
                        val end = Math.min(startingBlockHeight + (i * batchSize), latestBlockHeight + 1)
                        loadBlockRange(downloadedBlockHeight..(end-1))
                        progress = Math.round(i/batches.toFloat() * 100)
                        progressChannel.send(progress)
                        downloadedBlockHeight = end
                        twig("finished batch $i\n")
                    }
                }
                progressChannel.cancel()
            } else {
                twig("no missing blocks to download!")
            }
            return downloadedBlockHeight
        }

        suspend fun getLatestBlockHeight(): Long = withContext(IO) {
            createStub().getLatestBlock(Service.ChainSpec.newBuilder().build()).height
        }

        suspend fun submitTransaction(raw: ByteArray) = withContext(IO) {
            val request = Service.RawTransaction.newBuilder().setData(ByteString.copyFrom(raw)).build()
            createStub().sendTransaction(request)
        }

        suspend fun streamBlocks(pollFrequencyMillis: Long = DEFAULT_POLL_INTERVAL, startingBlockHeight: Long = Long.MAX_VALUE) = withContext(IO) {
            twig("streamBlocks started at $startingBlockHeight with interval $pollFrequencyMillis")
            // start with the next block, unless we were asked to start before then
            var nextBlockHeight = Math.min(startingBlockHeight, getLatestBlockHeight() + 1)
            while (isActive && !compactBlockChannel.isClosedForSend) {
                retryUpTo(DEFAULT_RETRIES) {
                    twig("polling for next block in stream on thread ${Thread.currentThread().name} . . .")
                    val latestBlockHeight = getLatestBlockHeight()
                    if (latestBlockHeight >= nextBlockHeight) {
                        twig("found a new block! (latest: $latestBlockHeight) on thread ${Thread.currentThread().name}")
                        loadBlockRange(nextBlockHeight..latestBlockHeight)
                        nextBlockHeight = latestBlockHeight + 1
                    } else {
                        twig("no new block yet (latest: $latestBlockHeight) on thread ${Thread.currentThread().name}")
                    }
                    twig("delaying $pollFrequencyMillis before polling for next block in stream")
                    delay(pollFrequencyMillis)
                }
            }
        }

        private suspend fun retryUpTo(retries: Int, initialDelay:Int = 10, block: suspend () -> Unit) {
            var failedAttempts = 0
            while (failedAttempts < retries) {
                try {
                    block()
                    return
                } catch (t: Throwable) {
                    failedAttempts++
                    if (failedAttempts >= retries) throw t
                    val duration = Math.pow(initialDelay.toDouble(), failedAttempts.toDouble()).toLong()
                    twig("failed due to $t retrying (${failedAttempts+1}/$retries) in ${duration}s...")
                    delay(duration)
                }
            }
        }

        suspend fun loadBlockRange(range: LongRange): Int = withContext(IO) {
            twig("requesting block range $range on thread ${Thread.currentThread().name}")
            val result = createStub(90_000L).getBlockRange(range.toBlockRange())
            twig("done requesting block range")
            var resultCount = 0
            while (checkNextBlock(result)) { //calls result.hasNext, which blocks because we use a blockingStub
                resultCount++
                val nextBlock = result.next()
                twig("...while loading block range $range, received new block ${nextBlock.height} on thread ${Thread.currentThread().name}. Sending...")
                compactBlockChannel.send(nextBlock)
                twig("...done sending block ${nextBlock.height}")
            }
            twig("done loading block range $range")
            resultCount
        }

        /* this helper method is used to allow for logic (like logging) before blocking on the current thread */
        private fun checkNextBlock(result: MutableIterator<CompactBlock>): Boolean {
            twig("awaiting next block...")
            return result.hasNext()
        }

        fun isClosed(): Boolean {
            return compactBlockChannel.isClosedForSend
        }

        override fun close() {
            compactBlockChannel.cancel()
            progressChannel.cancel()
            syncJob?.cancel()
            syncJob = null
            job?.cancel()
            job = null
        }
    }
}