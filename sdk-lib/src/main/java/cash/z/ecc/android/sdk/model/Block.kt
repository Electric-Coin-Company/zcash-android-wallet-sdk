package cash.z.ecc.android.sdk.model

data class Block(
    val height: BlockHeight,
    val hash: FirstClassByteArray,
    val time: Int,
    val saplingTree: FirstClassByteArray
)
