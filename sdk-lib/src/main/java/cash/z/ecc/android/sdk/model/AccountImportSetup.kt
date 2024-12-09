package cash.z.ecc.android.sdk.model

/**
 * Wrapper for the import account API based on viewing key
 *
 * @param accountName A human-readable name for the account
 * @param keySource A string identifier or other metadata describing the source of the seed
 * @param ufvk The UFVK used to detect transactions involving the account
 */
data class AccountImportSetup(
    val accountName: String,
    val keySource: String?,
    val ufvk: UnifiedFullViewingKey,
)
