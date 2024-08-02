package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.BlockHeight

internal data class BlockBatch(
    val order: Long,
    val range: ClosedRange<BlockHeight>,
    val size: Long,
    var blocks: List<JniBlockMeta>? = null
) {
    override fun toString() =
        "BlockBatch(order=$order, range=$range, size=$size${blocks?.let { ", blocks=${blocks!!.size}" } ?: ""})"
}
