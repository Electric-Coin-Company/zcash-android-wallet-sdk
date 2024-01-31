package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi

internal data class AccountBalance(
    val sapling: WalletBalance,
    val orchard: WalletBalance,
    val unshielded: Zatoshi
) {
    companion object {
        fun new(jni: JniAccountBalance): AccountBalance {
            return AccountBalance(
                sapling =
                    WalletBalance(
                        Zatoshi(jni.saplingVerifiedBalance + jni.saplingChangePending + jni.saplingValuePending),
                        Zatoshi(jni.saplingVerifiedBalance)
                    ),
                orchard =
                    WalletBalance(
                        Zatoshi(jni.orchardVerifiedBalance + jni.orchardChangePending + jni.orchardValuePending),
                        Zatoshi(jni.orchardVerifiedBalance)
                    ),
                unshielded = Zatoshi(jni.unshieldedBalance)
            )
        }
    }
}
