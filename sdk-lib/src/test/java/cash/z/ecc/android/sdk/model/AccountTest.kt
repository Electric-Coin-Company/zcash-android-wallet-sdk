package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.fixture.AccountFixture
import org.junit.Test
import java.util.UUID
import kotlin.test.assertFailsWith

class AccountTest {
    @Test
    fun uuid_wrong_length() {
        assertFailsWith(IllegalArgumentException::class) {
            AccountFixture.new(accountUuid = UUID.fromString("random"))
        }
    }
}
