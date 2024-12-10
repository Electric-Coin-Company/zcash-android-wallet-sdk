package cash.z.ecc.android.sdk.model

/**
 * Wrapper for the import account API based on viewing key. Set both [seedFingerprint] and [zip32AccountIndex] null
 * when using [AccountPurpose.ViewOnly].
 *
 * @param accountName A human-readable name for the account
 * @param keySource A string identifier or other metadata describing the source of the seed
 * @param seedFingerprint The [ZIP 32 seed fingerprint](https://zips.z.cash/zip-0032#seed-fingerprints)
 * @param ufvk The UFVK used to detect transactions involving the account
 * @param zip32AccountIndex The ZIP 32 account-level component of the HD derivation path at which to derive the
 * account's UFVK.
 */
data class AccountImportSetup(
    val accountName: String,
    val keySource: String?,
    val seedFingerprint: ByteArray?,
    val ufvk: UnifiedFullViewingKey,
    val zip32AccountIndex: Zip32AccountIndex?,
)
