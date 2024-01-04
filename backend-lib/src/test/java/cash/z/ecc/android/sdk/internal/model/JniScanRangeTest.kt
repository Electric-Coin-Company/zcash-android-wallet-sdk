package cash.z.ecc.android.sdk.internal.model

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class JniScanRangeTest {
    @Test
    fun attributes_within_constraints() {
        val instance =
            JniScanRange(
                startHeight = UInt.MIN_VALUE.toLong(),
                endHeight = UInt.MAX_VALUE.toLong(),
                priority = 10
            )
        assertIs<JniScanRange>(instance)
    }

    @Test
    fun attributes_not_in_constraints() {
        assertFailsWith(IllegalArgumentException::class) {
            JniScanRange(
                startHeight = Long.MIN_VALUE,
                endHeight = Long.MAX_VALUE,
                priority = 10
            )
        }
    }
}
