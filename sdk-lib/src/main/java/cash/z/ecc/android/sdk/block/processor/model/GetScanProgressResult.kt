package cash.z.ecc.android.sdk.block.processor.model

import cash.z.ecc.android.sdk.internal.model.ScanProgress
import cash.z.ecc.android.sdk.model.PercentDecimal

/**
 * Internal class for sharing get scan progress action result.
 */
internal sealed class GetScanProgressResult {
    data class Success(val scanProgress: ScanProgress) : GetScanProgressResult() {
        fun toPercentDecimal() = PercentDecimal(scanProgress.getSafeRation())
    }

    data object None : GetScanProgressResult()
    data class Failure(val exception: Throwable) : GetScanProgressResult()
}
