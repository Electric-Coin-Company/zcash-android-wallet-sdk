package cash.z.ecc.android.sdk.block.processor.model

/**
 * Progress model class for sharing the whole batch synchronization progress out of the synchronization process.
 */
internal data class BatchSyncProgress(
    val order: Long = 0,
    val resultState: SyncingResult =
        SyncingResult.AllSuccess
)
