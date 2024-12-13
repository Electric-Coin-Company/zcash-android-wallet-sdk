package cash.z.ecc.android.sdk.model

/**
 * An enumeration used to control what information is tracked by the wallet for notes received by a given account
 */
sealed class AccountPurpose {
    /**
     * Constant value that uniquely identifies this enum across FFI
     */
    abstract val value: Int

    /**
     * For spending accounts, the wallet will track information needed to spend received notes
     *
     * @param seedFingerprint The [ZIP 32 seed fingerprint](https://zips.z.cash/zip-0032#seed-fingerprints)
     * @param zip32AccountIndex The ZIP 32 account-level component of the HD derivation path at which to derive the
     */
    data class Spending(
        val seedFingerprint: ByteArray,
        val zip32AccountIndex: Zip32AccountIndex,
    ) : AccountPurpose() {
        override val value = 0
    }

    /**
     * For view-only accounts, the wallet will not track spend information
     */
    data object ViewOnly : AccountPurpose() {
        override val value = 1
    }
}
