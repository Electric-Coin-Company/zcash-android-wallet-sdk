package cash.z.ecc.android.sdk.block.processor.model

import cash.z.ecc.android.sdk.model.BlockHeight

/**
 * Internal class for sharing update chain tip action result.
 */
internal sealed class UpdateChainTipResult {
    data class Success(
        val height: BlockHeight
    ) : UpdateChainTipResult()

    data class Failure(
        val failedAtHeight: BlockHeight,
        val exception: Throwable
    ) : UpdateChainTipResult()
}
