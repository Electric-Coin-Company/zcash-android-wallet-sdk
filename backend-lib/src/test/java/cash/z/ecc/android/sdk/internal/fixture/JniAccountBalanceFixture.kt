package cash.z.ecc.android.sdk.internal.fixture

import cash.z.ecc.android.sdk.internal.model.JniAccountBalance

object JniAccountBalanceFixture {
    const val ACCOUNT_ID: Int = 0
    const val SAPLING_TOTAL_BALANCE: Long = 0L
    const val SAPLING_VERIFIED_BALANCE: Long = 0L
    const val ORCHARD_TOTAL_BALANCE: Long = 0L
    const val ORCHARD_VERIFIED_BALANCE: Long = 0L
    const val UNSHIELDED_BALANCE: Long = 0L

    @Suppress("LongParameterList")
    fun new(
        account: Int = ACCOUNT_ID,
        saplingTotalBalance: Long = SAPLING_TOTAL_BALANCE,
        saplingVerifiedBalance: Long = SAPLING_VERIFIED_BALANCE,
        orchardTotalBalance: Long = ORCHARD_TOTAL_BALANCE,
        orchardVerifiedBalance: Long = ORCHARD_VERIFIED_BALANCE,
        unshieldedBalance: Long = UNSHIELDED_BALANCE,
    ) = JniAccountBalance(
        account = account,
        saplingTotalBalance = saplingTotalBalance,
        saplingVerifiedBalance = saplingVerifiedBalance,
        orchardTotalBalance = orchardTotalBalance,
        orchardVerifiedBalance = orchardVerifiedBalance,
        unshieldedBalance = unshieldedBalance
    )
}
