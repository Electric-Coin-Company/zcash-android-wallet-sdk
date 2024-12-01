package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.model.Account

object AccountFixture {
    val ACCOUNT_UUID = WalletFixture.Alice.accounts[0].accountUuid
    const val ZIP_32_ACCOUNT_INDEX = 0

    fun new(accountUuid: ByteArray = ACCOUNT_UUID) =
        Account(
            accountUuid = accountUuid
        )
}
