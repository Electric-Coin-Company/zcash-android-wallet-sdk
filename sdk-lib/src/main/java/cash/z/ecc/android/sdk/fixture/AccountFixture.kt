package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.model.Account

object AccountFixture {
    val DEFAULT_UUID: ByteArray = "test_uuid".toByteArray()

    fun new(
        accountUuid: ByteArray = DEFAULT_UUID
    ) = Account(
        accountUuid = accountUuid
    )
}
