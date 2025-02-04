package cash.z.ecc.android.sdk.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AccountUuidTest {

    @Test
    fun uuid_wrong_length_test() {
        assertFailsWith(IllegalArgumentException::class) {
            AccountUuid.new("random".toByteArray())
        }
    }

    @Test
    fun uuid_correct_length_test() {
        // Testing that we can pass the [AccountUuid] creation requirements
        val accountUuid = AccountUuid.new(AccountUuidFixture.ACCOUNT_UUID_BYTE_ARRAY)
        assert(accountUuid.value.isNotEmpty())
    }

    @Test
    fun account_uuid_creation_test() {
        val accountUuid = AccountUuid.new(AccountUuidFixture.ACCOUNT_UUID_BYTE_ARRAY)
        assertEquals(AccountUuidFixture.ACCOUNT_UUID_BYTE_ARRAY, accountUuid.value)
    }

    @Test
    fun to_uuid_string_test() {
        val accountUuid = AccountUuid.new(AccountUuidFixture.ACCOUNT_UUID_BYTE_ARRAY)
        assertEquals(AccountUuidFixture.ACCOUNT_UUID_STRING, accountUuid.toUuidString())
    }
}
