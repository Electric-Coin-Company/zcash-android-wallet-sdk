package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi

internal data class AccountBalance(
    val sapling: WalletBalance
) {
    companion object {
        fun new(jni: JniAccountBalance): AccountBalance {
            return AccountBalance(
                sapling =
                    WalletBalance(
                        Zatoshi(jni.saplingTotalBalance),
                        Zatoshi(jni.saplingVerifiedBalance)
                    )
            )
        }
    }
}
