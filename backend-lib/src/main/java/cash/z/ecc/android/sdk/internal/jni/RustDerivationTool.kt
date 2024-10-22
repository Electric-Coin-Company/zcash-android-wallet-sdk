package cash.z.ecc.android.sdk.internal.jni

import cash.z.ecc.android.sdk.internal.Derivation
import cash.z.ecc.android.sdk.internal.model.JniUnifiedSpendingKey

class RustDerivationTool private constructor() : Derivation {
    override fun deriveUnifiedFullViewingKeys(
        seed: ByteArray,
        networkId: Int,
        numberOfAccounts: Int
    ): Array<String> = deriveUnifiedFullViewingKeysFromSeed(seed, numberOfAccounts, networkId = networkId)

    override fun deriveUnifiedFullViewingKey(
        usk: JniUnifiedSpendingKey,
        networkId: Int
    ): String = deriveUnifiedFullViewingKey(usk.bytes, networkId = networkId)

    override fun deriveUnifiedSpendingKey(
        seed: ByteArray,
        networkId: Int,
        accountIndex: Int
    ): JniUnifiedSpendingKey = deriveSpendingKey(seed, accountIndex, networkId = networkId)

    override fun deriveUnifiedAddress(
        seed: ByteArray,
        networkId: Int,
        accountIndex: Int
    ): String = deriveUnifiedAddressFromSeed(seed, accountIndex = accountIndex, networkId = networkId)

    /**
     * Given a Unified Full Viewing Key string, return the associated Unified Address.
     *
     * @param viewingKey the viewing key to use for deriving the address. The viewing key is tied to
     * a specific account so no account index is required.
     *
     * @return the address that corresponds to the viewing key.
     */
    override fun deriveUnifiedAddress(
        viewingKey: String,
        networkId: Int
    ): String = deriveUnifiedAddressFromViewingKey(viewingKey, networkId = networkId)

    override fun deriveArbitraryWalletKey(
        contextString: ByteArray,
        seed: ByteArray
    ): ByteArray = deriveArbitraryWalletKeyFromSeed(contextString, seed)

    override fun deriveArbitraryAccountKey(
        contextString: ByteArray,
        seed: ByteArray,
        networkId: Int,
        accountIndex: Int
    ): ByteArray = deriveArbitraryAccountKeyFromSeed(contextString, seed, accountIndex = accountIndex, networkId = networkId)

    companion object {
        suspend fun new(): Derivation {
            RustBackend.loadLibrary()

            return RustDerivationTool()
        }

        @JvmStatic
        private external fun deriveSpendingKey(
            seed: ByteArray,
            account: Int,
            networkId: Int
        ): JniUnifiedSpendingKey

        @JvmStatic
        private external fun deriveUnifiedFullViewingKeysFromSeed(
            seed: ByteArray,
            numberOfAccounts: Int,
            networkId: Int
        ): Array<String>

        @JvmStatic
        private external fun deriveUnifiedFullViewingKey(
            usk: ByteArray,
            networkId: Int
        ): String

        @JvmStatic
        private external fun deriveUnifiedAddressFromSeed(
            seed: ByteArray,
            accountIndex: Int,
            networkId: Int
        ): String

        @JvmStatic
        private external fun deriveUnifiedAddressFromViewingKey(
            key: String,
            networkId: Int
        ): String

        @JvmStatic
        private external fun deriveArbitraryWalletKeyFromSeed(
            contextString: ByteArray,
            seed: ByteArray
        ): ByteArray

        @JvmStatic
        private external fun deriveArbitraryAccountKeyFromSeed(
            contextString: ByteArray,
            seed: ByteArray,
            accountIndex: Int,
            networkId: Int
        ): ByteArray
    }
}
