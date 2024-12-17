package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.AccountBalance
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.BlockHeight

internal data class WalletSummary(
    val accountBalances: Map<AccountUuid, AccountBalance>,
    val chainTipHeight: BlockHeight,
    val fullyScannedHeight: BlockHeight,
    val scanProgress: ScanProgress,
    val nextSaplingSubtreeIndex: UInt,
    val nextOrchardSubtreeIndex: UInt
) {
    companion object {
        fun new(jni: JniWalletSummary): WalletSummary {
            return WalletSummary(
                accountBalances =
                    jni.accountBalances.associateBy(
                        { AccountUuid.new(it.accountUuid) },
                        { AccountBalance.new(it) }
                    ),
                chainTipHeight = BlockHeight(jni.chainTipHeight),
                fullyScannedHeight = BlockHeight(jni.fullyScannedHeight),
                scanProgress = ScanProgress.new(jni),
                nextSaplingSubtreeIndex = jni.nextSaplingSubtreeIndex.toUInt(),
                nextOrchardSubtreeIndex = jni.nextOrchardSubtreeIndex.toUInt()
            )
        }
    }
}
