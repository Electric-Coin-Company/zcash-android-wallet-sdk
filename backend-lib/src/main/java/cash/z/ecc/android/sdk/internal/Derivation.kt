package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.model.JniMetadataKey
import cash.z.ecc.android.sdk.internal.model.JniUnifiedSpendingKey

interface Derivation {
    fun deriveUnifiedAddress(
        viewingKey: String,
        networkId: Int
    ): String

    fun deriveUnifiedAddress(
        seed: ByteArray,
        networkId: Int,
        accountIndex: Long
    ): String

    fun deriveUnifiedSpendingKey(
        seed: ByteArray,
        networkId: Int,
        accountIndex: Long
    ): ByteArray

    /**
     * @return a unified full viewing key.
     */
    fun deriveUnifiedFullViewingKey(
        usk: JniUnifiedSpendingKey,
        networkId: Int
    ): String

    /**
     * @param numberOfAccounts Use [DEFAULT_NUMBER_OF_ACCOUNTS] to derive a single key.
     * @return an array of unified full viewing keys, one for each account.
     */
    fun deriveUnifiedFullViewingKeys(
        seed: ByteArray,
        networkId: Int,
        numberOfAccounts: Int
    ): Array<String>

    /**
     * Derives a ZIP 325 Account Metadata Key from the given seed.
     *
     * @return an account metadata key.
     */
    fun deriveAccountMetadataKey(
        seed: ByteArray,
        networkId: Int,
        accountIndex: Long
    ): JniMetadataKey

    /**
     * Derives a metadata key for private use from a ZIP 325 Account Metadata Key.
     *
     * If `ufvk` is non-null, this method will return one metadata key for every FVK item
     * contained within the UFVK, in preference order. As UFVKs may in general change over
     * time (due to the inclusion of new higher-preference FVK items, or removal of older
     * deprecated FVK items), private usage of these keys should always follow preference
     * order:
     * - For encryption-like private usage, the first key in the array should always be
     *   used, and all other keys ignored.
     * - For decryption-like private usage, each key in the array should be tried in turn
     *   until metadata can be recovered, and then the metadata should be re-encrypted
     *   under the first key.
     *
     * @param ufvk the external UFVK for which a metadata key is required, or `null` if the
     *        metadata key is "inherent" (for the same account as the Account Metadata Key).
     * @param privateUseSubject a globally unique non-empty sequence of at most 252 bytes that
     *        identifies the desired private-use context.
     * @return an array of 32-byte metadata keys in preference order.
     */
    fun derivePrivateUseMetadataKey(
        accountMetadataKey: JniMetadataKey,
        ufvk: String?,
        networkId: Int,
        privateUseSubject: ByteArray
    ): Array<ByteArray>

    /**
     * Derives a ZIP 32 Arbitrary Key from the given seed at the "wallet level", i.e.
     * directly from the seed with no ZIP 32 path applied.
     *
     * The resulting key will be the same across all networks (Zcash mainnet, Zcash
     * testnet, OtherCoin mainnet, and so on). You can think of it as a context-specific
     * seed fingerprint that can be used as (static) key material.
     *
     * @param contextString a globally-unique non-empty sequence of at most 252 bytes that
     *        identifies the desired context.
     * @return an array of 32 bytes.
     */
    fun deriveArbitraryWalletKey(
        contextString: ByteArray,
        seed: ByteArray
    ): ByteArray

    /**
     * Derives a ZIP 32 Arbitrary Key from the given seed at the account level.
     *
     * @param contextString a globally-unique non-empty sequence of at most 252 bytes that
     *        identifies the desired context.
     * @return an array of 32 bytes.
     */
    fun deriveArbitraryAccountKey(
        contextString: ByteArray,
        seed: ByteArray,
        networkId: Int,
        accountIndex: Long
    ): ByteArray

    companion object {
        const val DEFAULT_NUMBER_OF_ACCOUNTS = 1
    }
}
