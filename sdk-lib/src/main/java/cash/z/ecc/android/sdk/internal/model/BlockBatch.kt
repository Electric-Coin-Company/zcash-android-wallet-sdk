package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.BlockHeight

internal data class BlockBatch(
    val order: Long,
    val range: ClosedRange<BlockHeight>,
    var blocks: List<JniBlockMeta>? = null
)
