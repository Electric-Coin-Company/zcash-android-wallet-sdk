package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.model.Account

object AccountFixture {
    val ACCOUNT_UUID = "random_uuid_16_b".toByteArray()
    const val ZIP_32_ACCOUNT_INDEX = 0

    fun new(accountUuid: ByteArray = ACCOUNT_UUID) =
        Account(
            accountUuid = accountUuid
        )
}
