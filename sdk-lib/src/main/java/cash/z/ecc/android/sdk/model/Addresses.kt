package cash.z.ecc.android.sdk.model

data class Addresses(val unifiedAddress: String, val transparentAddress: String) {
    // Override to prevent leaking details in logs
    override fun toString() = "Addresses"
}
