package cash.z.ecc.android.sdk.model

data class Sent(
    val id: Int? = 0,
    val transactionId: Long = 0,
    val outputIndex: Int = 0,
    val account: Int = 0,
    val address: String,
    val value: Long = 0,
    val memo: FirstClassByteArray?
)
