package cash.z.ecc.android.sdk.block.processor.model

import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor

/**
 * Progress model class for sharing the whole batch synchronization progress out of the synchronization process.
 */
internal data class BatchSyncProgress(
    val inRangeOrder: Long = 0,
    val overallOrder: Long = 0,
    val resultState: CompactBlockProcessor.Companion.SyncingResult = CompactBlockProcessor.Companion.SyncingResult.AllSuccess
)