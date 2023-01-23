package co.electriccoin.lightwallet.client.fixture

import androidx.test.filters.SmallTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BlockRangeFixtureTest {

    @Test
    @SmallTest
    fun compare_default_values() {
        BlockRangeFixture.new().also {
            assertEquals(BlockRangeFixture.BLOCK_HEIGHT_LOWER_BOUND, it.start)
            assertEquals(BlockRangeFixture.BLOCK_HEIGHT_UPPER_BOUND, it.endInclusive)
        }
    }
}
