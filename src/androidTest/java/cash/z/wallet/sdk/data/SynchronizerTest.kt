package cash.z.wallet.sdk.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.junit.AfterClass
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import cash.z.wallet.sdk.rpc.CompactFormats

class SynchronizerTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun testSynchronizerExists() {
        assertNotNull(synchronizer)
    }

    @Test
    fun testBlockSaving() {
//        synchronizer.saveBlocks()
    }
    @Test
    fun testBlockScanning() {
        Thread.sleep(180000L)
    }
    private fun printFailure(result: Result<CompactFormats.CompactBlock>): String {
        return if (result.isFailure) "result failed due to: ${result.exceptionOrNull()!!.let { "$it caused by: ${it.cause}" }}}"
        else "success"
    }

    companion object {
        val job = Job()
        val testScope = CoroutineScope(Dispatchers.IO + job)
        val synchronizer = Synchronizer(ApplicationProvider.getApplicationContext(), testScope)

        @BeforeClass
        @JvmStatic
        fun setup() {
            synchronizer.start()
        }

        @AfterClass
        @JvmStatic
        fun close() {
            synchronizer.stop()
            testScope.cancel()
        }
    }
}
