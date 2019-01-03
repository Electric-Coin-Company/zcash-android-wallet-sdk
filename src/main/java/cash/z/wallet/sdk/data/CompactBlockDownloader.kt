package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.ext.debug
import cash.z.wallet.sdk.ext.toBlockHeight
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import rpc.CompactFormats.CompactBlock
import rpc.CompactTxStreamerGrpc
import rpc.Service
import java.io.Closeable

/**
 * Downloads compact blocks to the database
 */
class CompactBlockDownloader(val scope: CoroutineScope) : CompactBlockSource {

    private var connection: Connection? = null

    override fun blocks(): ReceiveChannel<Result<CompactBlock>> = connection!!.subscribe()

    fun start() {
        connection = Connection()
        scope.launch {
            connection!!.loadBlockRange(373070L..373085L).join()
            connection = Connection().open()
        }
    }

    fun stop() {
        connection?.close()
        connection = null
    }
    
    inner class Connection: Closeable {
        private var job: Job? = null
        private var syncJob: Job? = null
        private val compactBlockChannel = BroadcastChannel<Result<CompactBlock>>(100)
        private val errorHandler: CoroutineExceptionHandler

        val channel = ManagedChannelBuilder.forAddress("10.0.2.2", 9067).usePlaintext().build()
        val blockingStub = CompactTxStreamerGrpc.newBlockingStub(channel)

        init {
            errorHandler = CoroutineExceptionHandler { _, error ->
                debug("handling error: $error")
                try {
                    debug("totally about to launch something sweet")
                    GlobalScope.launch {
                        debug("sending error")
                        compactBlockChannel.send(Result.failure(error))
                        debug("error sent")
                    }

                }catch (t:Throwable) {
                    debug("failed to send error because of $t")
                }
            }
        }

        fun subscribe() = compactBlockChannel.openSubscription()

        fun loadBlockRange(range: LongRange) : Job {
            syncJob = scope.launch {
                if (isActive) {
                    debug("requesting a block range: ...")
                    channel.
                    val result = blockingStub.getBlockRange(
                        Service.BlockRange.newBuilder()
                            .setStart(range.first.toBlockHeight())
                            .setEnd(range.last.toBlockHeight())
                            .build()
                    )
                    while (result.hasNext()) {
                        try {
                            val nextBlock = result.next()
                            debug("received new block in range: ${nextBlock.height}")

                            async { debug("sending block from range: ${nextBlock.height}"); compactBlockChannel.send(Result.success(nextBlock)); debug("done sending block from range: ${nextBlock.height}") }
                        } catch (t: Throwable) {
                            async { debug("sending failure"); compactBlockChannel.send(Result.failure(t)); debug("done sending failure"); }
                        }
                    }
                }
            }
            return syncJob!!
        }

        fun open(): Connection {
            // TODO: use CoroutineScope.open to avoid the need to pass scope around
            job = scope.launch {
                var lastHeight = 0L
                while (isActive) {
debug("requesting a block...")
                    val result  = blockingStub.getLatestBlock(Service.ChainSpec.newBuilder().build())
                    if (result.height > lastHeight) {
debug("received new block: ${result.height}")
                        // if we have new data, send it and then wait a while
                        try {

                            async { debug("sending block: ${result.height}"); compactBlockChannel.send(Result.success(blockingStub.getBlock(result)));  debug("done sending block: ${result.height}");  }
                        } catch (t: Throwable) {
                            async { debug("sending failure"); compactBlockChannel.send(Result.failure(t));  debug("done sending failure");  }
                        }

                        lastHeight = result.height
                        delay(25 * 1000)

                    } else {
debug("received same old block: ${result.height}")
                        // otherwise keep checking fairly often until we have new data
                        delay(3000)
                    }
                }
            }
            return this
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