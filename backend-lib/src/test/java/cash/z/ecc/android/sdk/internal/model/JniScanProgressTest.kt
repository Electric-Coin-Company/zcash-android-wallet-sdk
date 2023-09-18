package cash.z.ecc.android.sdk.internal.model

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class JniScanProgressTest {
    @Test
    fun both_attribute_within_constraints() {
        val instance = JniScanProgress(
            numerator = 1L,
            denominator = 100L
        )
        assertIs<JniScanProgress>(instance)
    }

    @Test
    fun numerator_attribute_not_in_constraints() {
        assertFailsWith(IllegalArgumentException::class) {
            JniScanProgress(
                numerator = -1L,
                denominator = 100L
            )
        }
    }

    @Test
    fun denominator_attribute_not_in_constraints() {
        assertFailsWith(IllegalArgumentException::class) {
            JniScanProgress(
                numerator = 1L,
                denominator = 0L
            )
        }
    }

    @Test
    fun ratio_not_in_constraints() {
        assertFailsWith(IllegalArgumentException::class) {
            JniScanProgress(
                numerator = 100L,
                denominator = 1L
            )
        }
    }

}
