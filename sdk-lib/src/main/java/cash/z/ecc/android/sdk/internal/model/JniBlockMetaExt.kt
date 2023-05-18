package cash.z.ecc.android.sdk.internal.model

import cash.z.wallet.sdk.internal.rpc.CompactFormats
import co.electriccoin.lightwallet.client.model.CompactBlockUnsafe

internal fun JniBlockMeta.Companion.new(
    block: CompactFormats.CompactBlock,
    outputs: CompactBlockUnsafe.CompactBlockOutputsCounts
): JniBlockMeta {
    return JniBlockMeta(
        height = block.height,
        hash = block.hash.toByteArray(),
        time = block.time.toLong(),
        saplingOutputsCount = outputs.saplingOutputsCount.toLong(),
        orchardOutputsCount = outputs.orchardActionsCount.toLong()
    )
}
