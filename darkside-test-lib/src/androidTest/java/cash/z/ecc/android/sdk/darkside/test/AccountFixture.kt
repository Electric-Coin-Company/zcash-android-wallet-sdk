package cash.z.ecc.android.sdk.darkside.test

import cash.z.ecc.android.sdk.model.Account

@Suppress("MagicNumber")
object AccountFixture {
    val ACCOUNT_UUID = "random_uuid_16_b".toByteArray()
    const val ZIP_32_ACCOUNT_INDEX = 0

    fun new(accountUuid: ByteArray = ACCOUNT_UUID) =
        Account(
            accountUuid = accountUuid
        )
}
