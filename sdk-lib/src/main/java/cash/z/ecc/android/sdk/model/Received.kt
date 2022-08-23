package cash.z.ecc.android.sdk.model

data class Received(
    val id: Int? = 0,
    val transactionId: Int = 0,
    val outputIndex: Int,
    val account: Int,
    val value: Long,
    val spent: Int?,
    val diversifier: FirstClassByteArray,
    val rcm: FirstClassByteArray,
    val nf: FirstClassByteArray,
    val isChange: Boolean,
    val memo: FirstClassByteArray?
)
