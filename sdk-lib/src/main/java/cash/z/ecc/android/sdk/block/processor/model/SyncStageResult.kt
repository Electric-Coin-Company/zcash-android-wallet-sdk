package cash.z.ecc.android.sdk.block.processor.model

import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor
import cash.z.ecc.android.sdk.internal.model.BlockBatch

/**
 * Common progress model class for sharing a batch synchronization stage result internally in the synchronization loop.
 */
internal data class SyncStageResult(
    val batch: BlockBatch,
    val stageResult: CompactBlockProcessor.Companion.SyncingResult
)