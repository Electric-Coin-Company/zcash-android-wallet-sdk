package cash.z.ecc.android.sdk.internal.model

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class JniBlockMetaTest {
    @Test
    fun attributes_within_constraints() {
        val instance = JniBlockMeta(
            height = UInt.MAX_VALUE.toLong(),
            hash = byteArrayOf(),
            time = 0L,
            saplingOutputsCount = UInt.MIN_VALUE.toLong(),
            orchardOutputsCount = UInt.MIN_VALUE.toLong()
        )
        assertIs<JniBlockMeta>(instance)
    }

    @Test
    fun attributes_not_in_constraints() {
        assertFailsWith(IllegalArgumentException::class) {
            JniBlockMeta(
                height = Long.MAX_VALUE,
                hash = byteArrayOf(),
                time = 0L,
                saplingOutputsCount = Long.MIN_VALUE,
                orchardOutputsCount = Long.MIN_VALUE
            )
        }
    }
}
