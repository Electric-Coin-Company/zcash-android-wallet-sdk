package cash.z.ecc.android.sdk.internal.fixture

import cash.z.ecc.android.sdk.internal.model.JniAccountBalance

object JniAccountBalanceFixture {
    const val ACCOUNT_ID: Int = 0
    const val SAPLING_VERIFIED_BALANCE: Long = 0L
    const val SAPLING_CHANGE_PENDING: Long = 0L
    const val SAPLING_VALUE_PENDING: Long = 0L
    const val ORCHARD_VERIFIED_BALANCE: Long = 0L
    const val ORCHARD_CHANGE_PENDING: Long = 0L
    const val ORCHARD_VALUE_PENDING: Long = 0L
    const val UNSHIELDED_BALANCE: Long = 0L

    @Suppress("LongParameterList")
    fun new(
        account: Int = ACCOUNT_ID,
        saplingVerifiedBalance: Long = SAPLING_VERIFIED_BALANCE,
        saplingChangePending: Long = SAPLING_CHANGE_PENDING,
        saplingValuePending: Long = SAPLING_VALUE_PENDING,
        orchardVerifiedBalance: Long = ORCHARD_VERIFIED_BALANCE,
        orchardChangePending: Long = ORCHARD_CHANGE_PENDING,
        orchardValuePending: Long = ORCHARD_VALUE_PENDING,
        unshieldedBalance: Long = UNSHIELDED_BALANCE,
    ) = JniAccountBalance(
        account = account,
        saplingVerifiedBalance = saplingVerifiedBalance,
        saplingChangePending = saplingChangePending,
        saplingValuePending = saplingValuePending,
        orchardVerifiedBalance = orchardVerifiedBalance,
        orchardChangePending = orchardChangePending,
        orchardValuePending = orchardValuePending,
        unshieldedBalance = unshieldedBalance,
    )
}
