package cash.z.ecc.android.sdk.model

import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.fixture.WalletFixture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class UnifiedSpendingKeyTest {
    @Test
    @SmallTest
    @OptIn(ExperimentalCoroutinesApi::class)
    fun factory_copies_bytes() =
        runTest {
            val spendingKey = WalletFixture.getUnifiedSpendingKey()
            val expected = spendingKey.copyBytes().copyOf()

            val bytes = spendingKey.copyBytes()
            val newSpendingKey = UnifiedSpendingKey.new(spendingKey.account, bytes)
            bytes.clear()

            assertContentEquals(expected, newSpendingKey.getOrThrow().copyBytes())
        }

    @Test
    @SmallTest
    @OptIn(ExperimentalCoroutinesApi::class)
    fun get_copies_bytes() =
        runTest {
            val spendingKey = WalletFixture.getUnifiedSpendingKey()

            val expected = spendingKey.copyBytes()
            val newSpendingKey = UnifiedSpendingKey.new(spendingKey.account, expected)

            newSpendingKey.getOrThrow().copyBytes().clear()

            assertContentEquals(expected, newSpendingKey.getOrThrow().copyBytes())
        }

    @Test
    @SmallTest
    @OptIn(ExperimentalCoroutinesApi::class)
    fun toString_does_not_leak() =
        runTest {
            assertEquals(
                "UnifiedSpendingKey(account=Account(value=0))",
                WalletFixture.getUnifiedSpendingKey().toString()
            )
        }
}

private fun ByteArray.clear() {
    for (i in indices) {
        this[i] = 0
    }
}
