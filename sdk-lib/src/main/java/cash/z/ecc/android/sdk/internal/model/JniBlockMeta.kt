package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.internal.storage.block.CompactBlockOutputsCounts
import cash.z.wallet.sdk.internal.rpc.CompactFormats.CompactBlock

class JniBlockMeta(
    val height: Long,
    val hash: ByteArray,
    val time: Long,
    val saplingOutputsCount: Long,
    val orchardOutputsCount: Long
) {
    companion object {
        internal fun new(block: CompactBlock, outputs: CompactBlockOutputsCounts): JniBlockMeta {
            return JniBlockMeta(
                height = block.height,
                hash = block.hash.toByteArray(),
                time = block.time.toLong(),
                saplingOutputsCount = outputs.saplingOutputsCount,
                orchardOutputsCount = outputs.orchardActionsCount
            )
        }
    }
}
