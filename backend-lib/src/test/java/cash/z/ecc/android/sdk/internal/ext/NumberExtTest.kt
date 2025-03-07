package cash.z.ecc.android.sdk.internal.ext

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NumberExtTest {
    @Test
    fun is_in_range_test() {
        assertTrue(1L.isInUIntRange())
        assertTrue(0L.isInUIntRange())
        assertTrue(UInt.MAX_VALUE.toLong().isInUIntRange())
        assertTrue(UInt.MIN_VALUE.toLong().isInUIntRange())
    }

    @Test
    fun is_not_in_range_test() {
        assertFalse(0L.minus(1L).isInUIntRange())
        assertFalse(Long.MAX_VALUE.isInUIntRange())
        assertFalse(Long.MIN_VALUE.isInUIntRange())
        assertFalse(
            UInt.MAX_VALUE
                .toLong()
                .plus(1L)
                .isInUIntRange()
        )
        assertFalse(
            UInt.MIN_VALUE
                .toLong()
                .minus(1L)
                .isInUIntRange()
        )
    }
}
