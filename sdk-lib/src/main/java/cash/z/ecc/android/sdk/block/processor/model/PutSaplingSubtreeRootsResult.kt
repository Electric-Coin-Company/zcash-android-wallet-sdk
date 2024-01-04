package cash.z.ecc.android.sdk.block.processor.model

import cash.z.ecc.android.sdk.model.BlockHeight

/**
 * Internal class for sharing put sapling subtree roots action result.
 */
internal sealed class PutSaplingSubtreeRootsResult {
    object Success : PutSaplingSubtreeRootsResult()

    data class Failure(val failedAtHeight: BlockHeight, val exception: Throwable) : PutSaplingSubtreeRootsResult()
}
