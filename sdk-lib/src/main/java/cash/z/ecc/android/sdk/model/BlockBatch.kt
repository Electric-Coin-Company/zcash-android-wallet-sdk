package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.internal.model.JniBlockMeta

data class BlockBatch(
    val index: Long,
    val range: ClosedRange<BlockHeight>,
    var blocks: List<JniBlockMeta>? = null
)
