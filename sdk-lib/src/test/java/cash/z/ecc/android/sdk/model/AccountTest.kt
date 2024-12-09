package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.fixture.AccountFixture
import org.junit.Test
import java.util.UUID
import kotlin.test.assertFailsWith

class AccountTest {
    @Test
    fun out_of_bounds() {
        assertFailsWith(IllegalArgumentException::class) {
            AccountFixture.new(accountUuid = UUID.fromString("random"))
        }
    }
}
