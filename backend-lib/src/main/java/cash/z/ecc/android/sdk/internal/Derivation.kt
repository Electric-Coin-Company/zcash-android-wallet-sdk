package cash.z.ecc.android.sdk.internal

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
