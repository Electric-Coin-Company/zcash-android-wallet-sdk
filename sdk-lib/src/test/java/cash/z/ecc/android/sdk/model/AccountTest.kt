package cash.z.ecc.android.sdk.model

import org.junit.Test
import kotlin.test.assertFailsWith

class AccountTest {
    @Test
    fun out_of_bounds() {
        assertFailsWith(IllegalArgumentException::class) {
            Account("random".toByteArray())
        }
    }
}
