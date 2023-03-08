package co.electriccoin.lightwallet.client.fixture

import androidx.test.filters.SmallTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BlockRangeFixtureTest {

    @Test
    @SmallTest
    fun compare_default_values() {
        BenchmarkingBlockRangeFixture.new().also {
            assertEquals(BenchmarkingBlockRangeFixture.BLOCK_HEIGHT_LOWER_BOUND, it.start)
            assertEquals(BenchmarkingBlockRangeFixture.BLOCK_HEIGHT_UPPER_BOUND, it.endInclusive)
        }
    }
}
