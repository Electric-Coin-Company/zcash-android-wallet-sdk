package cash.z.ecc.android.sdk.internal.model

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class JniWalletSummaryTest {
    @Test
    fun both_attribute_within_constraints() {
        val instance = JniWalletSummary(
            accountBalances = Array(1) { JniAccountBalance(it, 0, 0) },
            progressNumerator = 1L,
            progressDenominator = 100L
        )
        assertIs<JniWalletSummary>(instance)
    }

    @Test
    fun numerator_attribute_not_in_constraints() {
        assertFailsWith(IllegalArgumentException::class) {
            JniWalletSummary(
                accountBalances = Array(1) { JniAccountBalance(it, 0, 0) },
                progressNumerator = -1L,
                progressDenominator = 100L
            )
        }
    }

    @Test
    fun denominator_attribute_not_in_constraints() {
        assertFailsWith(IllegalArgumentException::class) {
            JniWalletSummary(
                accountBalances = Array(1) { JniAccountBalance(it, 0, 0) },
                progressNumerator = 1L,
                progressDenominator = 0L
            )
        }
    }

    @Test
    fun ratio_not_in_constraints() {
        assertFailsWith(IllegalArgumentException::class) {
            JniWalletSummary(
                accountBalances = Array(1) { JniAccountBalance(it, 0, 0) },
                progressNumerator = 100L,
                progressDenominator = 1L
            )
        }
    }
}
