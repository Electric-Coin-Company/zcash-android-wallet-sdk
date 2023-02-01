package co.electriccoin.lightwallet.client.ext

import androidx.test.filters.SmallTest
import co.electriccoin.lightwallet.client.BuildConfig
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BenchmarkingExtTest {

    @Test
    @SmallTest
    fun check_build_config() {
        val benchmarkType = "benchmark" // $NON-NLS

        if (BuildConfig.BUILD_TYPE.contains(benchmarkType)) {
            assertTrue(BenchmarkingExt.isBenchmarking())
        } else {
            assertFalse(BenchmarkingExt.isBenchmarking())
        }
    }
}
