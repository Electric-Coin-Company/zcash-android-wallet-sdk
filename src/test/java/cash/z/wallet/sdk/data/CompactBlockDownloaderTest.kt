package cash.z.wallet.sdk.data

import cash.z.wallet.anyNotNull
import cash.z.wallet.sdk.ext.toBlockHeight
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import rpc.CompactFormats
import rpc.CompactTxStreamerGrpc.CompactTxStreamerBlockingStub
import rpc.Service
import kotlin.system.measureTimeMillis

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT) // allows us to setup the blockingStub once, with everything, rather than using custom stubs for each test
class CompactBlockDownloaderTest {

    lateinit var downloader: CompactBlockDownloader
    lateinit var connection: CompactBlockDownloader.Connection
    val job = Job()
    val io = CoroutineScope(Dispatchers.IO + job)

    @BeforeEach
    fun setUp(@Mock blockingStub: CompactTxStreamerBlockingStub) {
        whenever(blockingStub.getLatestBlock(any())).doAnswer {
            getLatestBlock()
        }
        // when asked for a block range, create an array of blocks and return an iterator over them with a slight delay between iterations
        whenever(blockingStub.getBlockRange(any())).doAnswer {
            val serviceRange = it.arguments[0] as Service.BlockRange
            val range = serviceRange.start.height..serviceRange.end.height
            val blocks = mutableListOf<CompactFormats.CompactBlock>()
            System.err.println("[Mock Connection] creating blocks in range: $range")
            for (i in range) {
                blocks.add(CompactFormats.CompactBlock.newBuilder().setHeight(i).build())
            }
            val blockIterator = blocks.iterator()

            val delayedIterator = object : Iterator<CompactFormats.CompactBlock> {
                override fun hasNext() = blockIterator.hasNext()

                override fun next(): CompactFormats.CompactBlock {
                    Thread.sleep(10L)
                    return blockIterator.next()
                }
            }
            delayedIterator
        }
        connection = spy(CompactBlockDownloader.Connection(blockingStub))
        downloader = CompactBlockDownloader(connection)
    }

    @AfterEach
    fun tearDown() {
        downloader.stop()
        io.cancel()
    }

    @Test
    fun `mock configuration sanity check`() = runBlocking<Unit> {
        assertEquals(getLatestBlock().height, connection.getLatestBlockHeight(), "Unexpected height. Verify that mocks are properly configured.")
    }

    @Test
    fun `downloading missing blocks happens in chunks`() = runBlocking<Unit> {
        val start = getLatestBlock().height - 31L
        val downloadCount = connection.downloadMissingBlocks(start, 10) - start
        assertEquals(32, downloadCount)

        verify(connection).getLatestBlockHeight()
        verify(connection).loadBlockRange(start..(start + 9L)) // a range of 10 block is requested
        verify(connection, times(4)).loadBlockRange(anyNotNull()) // 4 batches are required
    }

    @Test
    fun `channel contains expected blocks`() = runBlocking {
        val mailbox = connection.subscribe()
        var blockCount = 0
        val start = getLatestBlock().height - 31L
        io.launch {
            connection.downloadMissingBlocks(start, 10)
            mailbox.cancel() // exits the for loop, below, once downloading is complete
        }
        for(block in mailbox) {
            println("got block with height ${block.height} on thread ${Thread.currentThread().name}")
            blockCount++
        }
        assertEquals(32, blockCount)
    }

    // lots of logging here because this is more of a sanity test for peace of mind
    @Test
    fun `streaming yields the latest blocks with proper timing`() = runBlocking {
        // just tweak these a bit for sanity rather than making a bunch of tests that would be slow
        val pollInterval = BLOCK_INTERVAL_MILLIS/2L
        val repetitions = 3

        println("${System.currentTimeMillis()} : starting with blockInterval $BLOCK_INTERVAL_MILLIS and pollInterval $pollInterval")
        val mailbox = connection.subscribe()
        io.launch {
            connection.streamBlocks(pollInterval)
        }
        // sync up with the block interval, first
        mailbox.receive()

        // now, get a few blocks and measure the expected time
        val deltaTime = measureTimeMillis {
            repeat(repetitions) {
                println("${System.currentTimeMillis()} : checking the mailbox on thread ${Thread.currentThread().name}...")
                val mail = mailbox.receive()
                println("${System.currentTimeMillis()} : ...got ${mail.height} in the mail! on thread ${Thread.currentThread().name}")
            }
        }
        val totalIntervals = repetitions * BLOCK_INTERVAL_MILLIS
        val bounds = (totalIntervals - pollInterval)..(totalIntervals + pollInterval)
        println("${System.currentTimeMillis()} : finished in $deltaTime and it was between $bounds")

        mailbox.cancel()
        assertTrue(bounds.contains(deltaTime), "Blocks received ${if(bounds.first < deltaTime) "slower" else "faster"} than expected. $deltaTime should be in the range of $bounds")
    }

    @Test
    fun `downloader gets missing blocks and then streams`() = runBlocking {
        val targetHeight = getLatestBlock().height + 3L
        val initialBlockHeight = targetHeight - 30L
        println("starting from $initialBlockHeight to $targetHeight")
        val mailbox = downloader.start(io, initialBlockHeight, 10, 500L)

        // receive from channel until we reach the target height, counting blocks along the way
        var firstBlock: CompactFormats.CompactBlock? = null
        var blockCount = 0
        do {
            println("waiting for block number $blockCount...")
            val block = mailbox.receive()
            println("...received block ${block.height} on thread ${Thread.currentThread().name}")
            blockCount++
            if (firstBlock == null) firstBlock = block
        } while (block.height < targetHeight)

        mailbox.cancel()
        assertEquals(firstBlock?.height, initialBlockHeight, "Failed to start at block $initialBlockHeight")
        assertEquals(targetHeight - initialBlockHeight + 1L, blockCount.toLong(), "Incorrect number of blocks, verify that there are no duplicates in the test output")
    }

    companion object {
        const val BLOCK_INTERVAL_MILLIS = 1000L

        private fun getLatestBlock(): Service.BlockID {
            // number of intervals that have passed (without rounding...)
            val intervalCount = System.currentTimeMillis() / BLOCK_INTERVAL_MILLIS
            return intervalCount.toBlockHeight()
        }
    }

}