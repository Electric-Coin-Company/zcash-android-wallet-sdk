package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.internal.model.JniAccountBalance

data class AccountBalance(
    val sapling: WalletBalance,
    val orchard: WalletBalance,
    val unshielded: Zatoshi
) {
    companion object {
        fun new(jni: JniAccountBalance): AccountBalance =
            AccountBalance(
                sapling =
                    WalletBalance(
                        available = Zatoshi(jni.saplingVerifiedBalance),
                        changePending = Zatoshi(jni.saplingChangePending),
                        valuePending = Zatoshi(jni.saplingValuePending)
                    ),
                orchard =
                    WalletBalance(
                        available = Zatoshi(jni.orchardVerifiedBalance),
                        changePending = Zatoshi(jni.orchardChangePending),
                        valuePending = Zatoshi(jni.orchardValuePending)
                    ),
                unshielded = Zatoshi(jni.unshieldedBalance)
            )
    }
}
