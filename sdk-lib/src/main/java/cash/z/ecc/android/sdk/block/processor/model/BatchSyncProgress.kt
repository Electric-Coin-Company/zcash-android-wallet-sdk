package cash.z.ecc.android.sdk.block.processor.model

import cash.z.ecc.android.sdk.model.BlockHeight

/**
 * Progress model class for sharing the whole batch synchronization progress out of the synchronization process.
 */
internal data class BatchSyncProgress(
    val order: Long = 0,
    val range: ClosedRange<BlockHeight>? = null,
    val resultState: SyncingResult = SyncingResult.AllSuccess
) {
    override fun toString() = "BatchSyncProgress(order=$order, range=$range, state=$resultState)"
}
