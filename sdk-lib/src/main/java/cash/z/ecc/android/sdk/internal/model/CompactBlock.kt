package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray

data class CompactBlock internal constructor(
    val height: BlockHeight,
    val data: FirstClassByteArray
) {
    companion object {
        fun new(height: BlockHeight, data: FirstClassByteArray): CompactBlock {
            // on this place we should validate input values from "unsafe" model version

            return CompactBlock(height, data)
        }
    }
}
