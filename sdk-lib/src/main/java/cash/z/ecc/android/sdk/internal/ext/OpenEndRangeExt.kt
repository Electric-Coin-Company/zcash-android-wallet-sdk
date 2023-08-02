package cash.z.ecc.android.sdk.internal.ext

import cash.z.ecc.android.sdk.model.BlockHeight

@OptIn(ExperimentalStdlibApi::class)
internal fun OpenEndRange<BlockHeight>.toClosedRange(): ClosedRange<BlockHeight> = start..endExclusive - 1
