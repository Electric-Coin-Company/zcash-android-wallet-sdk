package cash.z.ecc.android.sdk.internal.model

internal data class JniBlockMeta(
    val height: Long,
    val hash: ByteArray,
    val time: Long,
    val saplingOutputsCount: Long,
    val orchardOutputsCount: Long
)
