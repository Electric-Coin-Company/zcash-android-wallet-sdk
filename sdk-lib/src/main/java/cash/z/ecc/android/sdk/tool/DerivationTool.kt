package cash.z.ecc.android.sdk.tool

import cash.z.ecc.android.sdk.internal.Derivation
import cash.z.ecc.android.sdk.internal.SuspendingLazy
import cash.z.ecc.android.sdk.internal.TypesafeDerivationToolImpl
import cash.z.ecc.android.sdk.internal.jni.RustDerivationTool
import cash.z.ecc.android.sdk.model.AccountMetadataKey
import cash.z.ecc.android.sdk.model.UnifiedFullViewingKey
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.model.Zip32AccountIndex

interface DerivationTool {
    /**
     * Given a seed and a number of accounts, return the associated Unified Full Viewing Keys.
     *
     * @param seed the seed from which to derive viewing keys.
     * @param numberOfAccounts the number of accounts to use.
     *
     * @return the UFVKs derived from the seed, encoded as Strings.
     */
    suspend fun deriveUnifiedFullViewingKeys(
        seed: ByteArray,
        network: ZcashNetwork,
        numberOfAccounts: Int
    ): List<UnifiedFullViewingKey>

    /**
     * Given a unified spending key, return the associated unified full viewing key.
     *
     * @param usk the key from which to derive the viewing key.
     *
     * @return a unified full viewing key.
     */
    suspend fun deriveUnifiedFullViewingKey(
        usk: UnifiedSpendingKey,
        network: ZcashNetwork
    ): UnifiedFullViewingKey

    /**
     * Derives and returns a unified spending key from the given seed for the given account ID.
     *
     * Returns the newly created [ZIP 316] account identifier, along with the binary encoding
     * of the [`UnifiedSpendingKey`] for the newly created account. The caller should store
     * the returned spending key in a secure fashion.
     *
     * @param seed the seed from which to derive spending keys.
     * @param accountIndex the ZIP 32 account index to derive.
     *
     * @return the unified spending key for the account.
     */
    suspend fun deriveUnifiedSpendingKey(
        seed: ByteArray,
        network: ZcashNetwork,
        accountIndex: Zip32AccountIndex
    ): UnifiedSpendingKey

    /**
     * Given a seed and account index, return the associated Unified Address.
     *
     * @param seed the seed from which to derive the address.
     * @param accountIndex the ZIP 32 account index to use for deriving the address.
     *
     * @return the address that corresponds to the seed and account index.
     */
    suspend fun deriveUnifiedAddress(
        seed: ByteArray,
        network: ZcashNetwork,
        accountIndex: Zip32AccountIndex
    ): String

    /**
     * Given a Unified Full Viewing Key string, return the associated Unified Address.
     *
     * @param viewingKey the viewing key to use for deriving the address. The viewing key is tied to
     * a specific account so no account index is required.
     *
     * @return the address that corresponds to the viewing key.
     */
    suspend fun deriveUnifiedAddress(
        viewingKey: String,
        network: ZcashNetwork
    ): String

    /**
     * Derives a ZIP 325 Account Metadata Key from the given seed.
     *
     * @return an account metadata key.
     */
    suspend fun deriveAccountMetadataKey(
        seed: ByteArray,
        network: ZcashNetwork,
        accountIndex: Zip32AccountIndex
    ): AccountMetadataKey

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
    suspend fun derivePrivateUseMetadataKey(
        accountMetadataKey: AccountMetadataKey,
        ufvk: String?,
        network: ZcashNetwork,
        privateUseSubject: ByteArray
    ): Array<ByteArray>

    /**
     * Derives a [ZIP 32 Arbitrary Key] from the given seed at the "wallet level", i.e.
     * directly from the seed with no ZIP 32 path applied.
     *
     * The resulting key will be the same across all networks (Zcash mainnet, Zcash
     * testnet, OtherCoin mainnet, and so on). You can think of it as a context-specific
     * seed fingerprint that can be used as (static) key material.
     *
     * [ZIP 32 Arbitrary Key]: https://zips.z.cash/zip-0032#specification-arbitrary-key-derivation
     *
     * @param contextString a globally-unique non-empty sequence of at most 252 bytes that
     *        identifies the desired context.
     * @return an array of 32 bytes.
     */
    suspend fun deriveArbitraryWalletKey(
        contextString: ByteArray,
        seed: ByteArray
    ): ByteArray

    /**
     * Derives a [ZIP 32 Arbitrary Key] from the given seed at the account level.
     *
     * [ZIP 32 Arbitrary Key]: https://zips.z.cash/zip-0032#specification-arbitrary-key-derivation
     *
     * @param contextString a globally-unique non-empty sequence of at most 252 bytes that
     *        identifies the desired context.
     * @param seed the seed from which to derive the arbitrary key.
     * @param accountIndex the ZIP 32 account index for which to derive the arbitrary key.
     * @return an array of 32 bytes.
     */
    suspend fun deriveArbitraryAccountKey(
        contextString: ByteArray,
        seed: ByteArray,
        network: ZcashNetwork,
        accountIndex: Zip32AccountIndex
    ): ByteArray

    companion object {
        const val DEFAULT_NUMBER_OF_ACCOUNTS = Derivation.DEFAULT_NUMBER_OF_ACCOUNTS

        private val instance =
            SuspendingLazy<Unit, DerivationTool> {
                TypesafeDerivationToolImpl(RustDerivationTool.new())
            }

        suspend fun getInstance() = instance.getInstance(Unit)
    }
}
