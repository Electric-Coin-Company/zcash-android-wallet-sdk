package cash.z.ecc.android.sdk.block.processor.model

import cash.z.ecc.android.sdk.model.BlockHeight

/**
 * Internal class for sharing get max scanned height action result.
 */
internal sealed class GetMaxScannedHeightResult {
    data class Success(
        val height: BlockHeight
    ) : GetMaxScannedHeightResult()

    data object None : GetMaxScannedHeightResult()

    data class Failure(
        val exception: Throwable
    ) : GetMaxScannedHeightResult()
}
