package cash.z.ecc.android.sdk.block.processor.model

import cash.z.ecc.android.sdk.internal.model.ScanRange
import cash.z.ecc.android.sdk.model.BlockHeight

/**
 * Internal class for sharing suggested scan ranges action result.
 */
internal sealed class SuggestScanRangesResult {
    data class Success(val ranges: List<ScanRange>) : SuggestScanRangesResult()
    data class Failure(val failedAtHeight: BlockHeight, val exception: Throwable) : SuggestScanRangesResult()
}
