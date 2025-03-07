package cash.z.ecc.android.sdk.block.processor.model

import cash.z.ecc.android.sdk.internal.model.ScanRange

/**
 * Internal class for sharing verify suggested scan range action result.
 */
internal sealed class VerifySuggestedScanRange {
    data class ShouldVerify(
        val scanRange: ScanRange
    ) : VerifySuggestedScanRange()

    object NoRangeToVerify : VerifySuggestedScanRange()
}
