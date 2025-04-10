package cash.z.ecc.android.sdk.block.processor.model

import cash.z.ecc.android.sdk.internal.model.WalletSummary
import cash.z.ecc.android.sdk.model.PercentDecimal

/**
 * Internal class for sharing wallet summary action result.
 */
internal sealed class GetWalletSummaryResult {
    data class Success(
        val walletSummary: WalletSummary
    ) : GetWalletSummaryResult() {
        fun scanProgressPercentDecimal() = PercentDecimal(walletSummary.scanProgress.getSafeRatio())

        fun recoveryProgressPercentDecimal() =
            walletSummary.recoveryProgress?.getSafeRatio()?.let { ratio -> PercentDecimal(ratio) }
    }

    data object None : GetWalletSummaryResult()

    data class Failure(
        val exception: Throwable
    ) : GetWalletSummaryResult()
}
