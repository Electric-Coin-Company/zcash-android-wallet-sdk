package cash.z.ecc.android.sdk.model

data class Account(
    val account: Int? = 0,
    val extendedFullViewingKey: String,
    val address: String,
    val transparentAddress: String
)
