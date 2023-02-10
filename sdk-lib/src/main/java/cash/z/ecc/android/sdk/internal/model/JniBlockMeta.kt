package cash.z.ecc.android.sdk.internal.model

class JniBlockMeta(
    val height: Long,
    val hash: ByteArray,
    val time: Long,
    val saplingOutputsCount: Long,
    val orchardActionsCount: Long
)
