package cash.z.ecc.android.sdk.tool

import cash.z.ecc.android.sdk.internal.Derivation
import cash.z.ecc.android.sdk.internal.SuspendingLazy
import cash.z.ecc.android.sdk.internal.TypesafeDerivationToolImpl
import cash.z.ecc.android.sdk.internal.jni.RustDerivationTool
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.UnifiedFullViewingKey
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.ZcashNetwork

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
     * @param account the account to derive.
     *
     * @return the unified spending key for the account.
     */
    suspend fun deriveUnifiedSpendingKey(
        seed: ByteArray,
        network: ZcashNetwork,
        account: Account
    ): UnifiedSpendingKey

    /**
     * Given a seed and account index, return the associated Unified Address.
     *
     * @param seed the seed from which to derive the address.
     * @param account the index of the account to use for deriving the address.
     *
     * @return the address that corresponds to the seed and account index.
     */
    suspend fun deriveUnifiedAddress(
        seed: ByteArray,
        network: ZcashNetwork,
        account: Account
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
     * @return an array of 32 bytes.
     */
    suspend fun deriveArbitraryAccountKey(
        contextString: ByteArray,
        seed: ByteArray,
        network: ZcashNetwork,
        account: Account
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
