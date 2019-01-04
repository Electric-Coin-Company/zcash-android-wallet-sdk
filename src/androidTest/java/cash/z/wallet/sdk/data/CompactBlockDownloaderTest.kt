package cash.z.wallet.sdk.data

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.*
import org.junit.AfterClass
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import rpc.CompactFormats

class CompactBlockDownloaderTestAndroid {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun testSynchronizerExists() {
        assertNotNull(downloader)
    }

    @Test
    fun testBlocks() = runBlocking {
        msg("about to receive (is the channel Closed? ${downloader.blocks().isClosedForReceive})")
        val result = downloader.blocks().receive()
        msg("donezo")
        assertTrue(printFailure(result), result.isSuccess)
    }

    private fun printFailure(result: Result<CompactFormats.CompactBlock>): String {
        return if (result.isFailure) "result failed due to: ${result.exceptionOrNull()!!.let { "$it caused by: ${it.cause}" }}}"
        else "success"
    }

    @Test
    fun testBlockHeight() = runBlocking {
        delay(200)
        val result = downloader.blocks().receive()
        assertTrue(printFailure(result), result.isSuccess)
        assertTrue("Unexpected height value", result.getOrThrow().height > 300000)
    }

    companion object {
        val job = Job()
        val testScope = CoroutineScope(Dispatchers.IO + job)
        val downloader = CompactBlockDownloader(testScope)

        @BeforeClass
        @JvmStatic
        fun setup() {
            downloader.start()
        }

        @AfterClass
        @JvmStatic
        fun close() {
            downloader.stop()
            job.cancel()
        }

        fun msg(message: String) {
            Log.e("DBUG", "[${Thread.currentThread().name}] $message")
        }
    }
}
