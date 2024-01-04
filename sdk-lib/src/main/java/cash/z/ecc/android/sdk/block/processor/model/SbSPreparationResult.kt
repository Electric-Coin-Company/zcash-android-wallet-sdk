package cash.z.ecc.android.sdk.block.processor.model

import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor
import cash.z.ecc.android.sdk.model.BlockHeight

/**
 * Internal class for sharing pre-synchronization steps result.
 */
internal sealed class SbSPreparationResult {
    object ConnectionFailure : SbSPreparationResult()

    data class ProcessFailure(
        val failedAtHeight: BlockHeight,
        val exception: Throwable
    ) : SbSPreparationResult() {
        fun toBlockProcessingResult(): CompactBlockProcessor.BlockProcessingResult =
            CompactBlockProcessor.BlockProcessingResult.SyncFailure(
                this.failedAtHeight,
                this.exception
            )
    }

    data class Success(
        val suggestedRangesResult: SuggestScanRangesResult,
        val verifyRangeResult: VerifySuggestedScanRange
    ) : SbSPreparationResult()

    object NoMoreBlocksToProcess : SbSPreparationResult()
}
