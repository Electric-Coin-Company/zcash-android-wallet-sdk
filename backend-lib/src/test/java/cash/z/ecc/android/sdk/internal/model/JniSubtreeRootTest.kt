package cash.z.ecc.android.sdk.internal.model

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class JniSubtreeRootTest {
    @Test
    fun attributes_within_constraints() {
        val instance =
            JniSubtreeRoot(
                rootHash = byteArrayOf(),
                completingBlockHeight = UInt.MAX_VALUE.toLong()
            )
        assertIs<JniSubtreeRoot>(instance)
    }

    @Test
    fun attributes_not_in_constraints() {
        assertFailsWith(IllegalArgumentException::class) {
            JniSubtreeRoot(
                rootHash = byteArrayOf(),
                completingBlockHeight = Long.MAX_VALUE
            )
        }
    }
}
