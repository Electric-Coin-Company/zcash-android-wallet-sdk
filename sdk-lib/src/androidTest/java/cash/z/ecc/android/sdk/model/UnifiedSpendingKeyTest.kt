package cash.z.ecc.android.sdk.model

import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.fixture.WalletFixture
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class UnifiedSpendingKeyTest {
    @Test
    @SmallTest
    fun factory_copies_bytes() =
        runTest {
            val spendingKey = WalletFixture.getUnifiedSpendingKey()
            val expected = spendingKey.copyBytes().copyOf()

            val bytes = spendingKey.copyBytes()
            val newSpendingKey = UnifiedSpendingKey.new(bytes)
            bytes.clear()

            assertContentEquals(expected, newSpendingKey.copyBytes())
        }

    @Test
    @SmallTest
    fun get_copies_bytes() =
        runTest {
            val spendingKey = WalletFixture.getUnifiedSpendingKey()

            val expected = spendingKey.copyBytes()
            val newSpendingKey = UnifiedSpendingKey.new(expected)

            newSpendingKey.copyBytes().clear()

            assertContentEquals(expected, newSpendingKey.copyBytes())
        }

    @Test
    @SmallTest
    fun toString_does_not_leak() =
        runTest {
            assertEquals(
                "UnifiedSpendingKey(bytes=***)",
                WalletFixture.getUnifiedSpendingKey().toString()
            )
        }
}

private fun ByteArray.clear() {
    for (i in indices) {
        this[i] = 0
    }
}
