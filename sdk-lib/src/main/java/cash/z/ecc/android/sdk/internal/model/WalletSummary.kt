package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.Account

internal data class WalletSummary(
    val accountBalances: Map<Account, AccountBalance>,
    val scanProgress: ScanProgress
) {
    companion object {
        fun new(jni: JniWalletSummary): WalletSummary {
            return WalletSummary(
                accountBalances =
                    jni.accountBalances.associateBy({ Account(it.account) }, {
                        AccountBalance.new(it)
                    }),
                scanProgress = ScanProgress.new(jni)
            )
        }
    }
}
