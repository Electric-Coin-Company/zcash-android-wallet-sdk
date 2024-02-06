package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi

object WalletBalanceFixture {
    val AVAILABLE: Long = 8L
    val CHANGE_PENDING: Long = 4
    val VALUE_PENDING: Long = 4

    fun new(
        available: Zatoshi = Zatoshi(AVAILABLE),
        changePending: Zatoshi = Zatoshi(CHANGE_PENDING),
        valuePending: Zatoshi = Zatoshi(VALUE_PENDING)
    ) = WalletBalance(
        available = available,
        changePending = changePending,
        valuePending = valuePending
    )

    fun newLong(
        available: Long = AVAILABLE,
        changePending: Long = CHANGE_PENDING,
        valuePending: Long = VALUE_PENDING
    ) = WalletBalance(
        available = Zatoshi(available),
        changePending = Zatoshi(changePending),
        valuePending = Zatoshi(valuePending)
    )
}
