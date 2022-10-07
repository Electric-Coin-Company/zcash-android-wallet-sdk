package cash.z.ecc.android.sdk.internal.model

internal data class AccountDetails(
    val account: Int,
    val extendedFullViewingKey: String,
    val address: String,
    val transparentAddress: String
) {
    // Override to prevent leaking details in logs
    override fun toString() = "Account"
}
