package co.electriccoin.lightwallet.client.model

import androidx.test.filters.SmallTest
import co.electriccoin.lightwallet.client.fixture.SingleCompactBlockFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompactBlockUnsafeTest {

    @Test
    @SmallTest
    fun serialize_test() {
        val block = SingleCompactBlockFixture.new()

        val serialized = block.toByteArray().also {
            assertTrue(it.isNotEmpty())
        }

        CompactBlockUnsafe.fromByteArray(serialized).also {
            assertEquals(SingleCompactBlockFixture.DEFAULT_PROTO_VERSION, it.protoVersion)

            assertEquals(SingleCompactBlockFixture.DEFAULT_HEIGHT, it.height)

            assertEquals(SingleCompactBlockFixture.DEFAULT_HASH,
                SingleCompactBlockFixture.fixtureDataToHeight(it.hash))

            assertEquals(SingleCompactBlockFixture.DEFAULT_PREV_HASH,
                SingleCompactBlockFixture.fixtureDataToHeight(it.prevHash))

            assertEquals(SingleCompactBlockFixture.DEFAULT_HEADER,
                SingleCompactBlockFixture.fixtureDataToHeight(it.header))

            assertEquals(SingleCompactBlockFixture.DEFAULT_TIME, it.time)

            assertEquals(SingleCompactBlockFixture.DEFAULT_VTX, it.vtx)
        }
    }
}