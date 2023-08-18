package cash.z.ecc.android.sdk.block.processor.model

import cash.z.ecc.android.sdk.internal.model.SubtreeRoot

/**
 * Internal class for get subtree roots action result.
 */
internal sealed class GetSubtreeRootsResult {
    // SbS: Spend-before-Sync
    data class UseSbS(val subTreeRootList: List<SubtreeRoot>) : GetSubtreeRootsResult()
    object UseLinear : GetSubtreeRootsResult()
    object FailureConnection : GetSubtreeRootsResult()
    data class OtherFailure(val exception: Throwable) : GetSubtreeRootsResult()
}
