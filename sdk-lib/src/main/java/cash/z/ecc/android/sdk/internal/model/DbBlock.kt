package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray

internal data class DbBlock(
    val height: BlockHeight,
    val hash: FirstClassByteArray,
    val blockTimeEpochSeconds: Long,
)
