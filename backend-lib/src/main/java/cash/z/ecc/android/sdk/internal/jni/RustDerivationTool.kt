package cash.z.ecc.android.sdk.internal.jni

import cash.z.ecc.android.sdk.internal.Derivation
import cash.z.ecc.android.sdk.internal.model.JniMetadataKey
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
        accountIndex: Long
    ): ByteArray = deriveSpendingKey(seed, accountIndex, networkId = networkId)

    override fun deriveUnifiedAddress(
        seed: ByteArray,
        networkId: Int,
        accountIndex: Long
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

    override fun deriveAccountMetadataKey(
        seed: ByteArray,
        networkId: Int,
        accountIndex: Long
    ): JniMetadataKey = deriveAccountMetadataKeyFromSeed(seed, accountIndex, networkId)

    override fun derivePrivateUseMetadataKey(
        accountMetadataKey: JniMetadataKey,
        ufvk: String?,
        networkId: Int,
        privateUseSubject: ByteArray
    ): Array<ByteArray> =
        derivePrivateUseMetadataKey(
            accountMetadataKey_sk = accountMetadataKey.sk,
            accountMetadataKey_c = accountMetadataKey.chainCode,
            ufvk,
            privateUseSubject,
            networkId
        )

    override fun deriveArbitraryWalletKey(
        contextString: ByteArray,
        seed: ByteArray
    ): ByteArray = deriveArbitraryWalletKeyFromSeed(contextString, seed)

    override fun deriveArbitraryAccountKey(
        contextString: ByteArray,
        seed: ByteArray,
        networkId: Int,
        accountIndex: Long
    ): ByteArray =
        deriveArbitraryAccountKeyFromSeed(
            contextString = contextString,
            seed = seed,
            accountIndex = accountIndex,
            networkId = networkId
        )

    companion object {
        suspend fun new(): Derivation {
            RustBackend.loadLibrary()

            return RustDerivationTool()
        }

        @JvmStatic
        private external fun deriveSpendingKey(
            seed: ByteArray,
            accountIndex: Long,
            networkId: Int
        ): ByteArray

        @JvmStatic
        private external fun deriveUnifiedFullViewingKeysFromSeed(
            seed: ByteArray,
            numberOfAccounts: Int,
            networkId: Int
        ): Array<String>

        @JvmStatic
        private external fun deriveUnifiedAddressFromSeed(
            seed: ByteArray,
            accountIndex: Long,
            networkId: Int
        ): String

        @JvmStatic
        private external fun deriveUnifiedAddressFromViewingKey(
            key: String,
            networkId: Int
        ): String

        @JvmStatic
        private external fun deriveUnifiedFullViewingKey(
            usk: ByteArray,
            networkId: Int
        ): String

        @JvmStatic
        private external fun deriveAccountMetadataKeyFromSeed(
            seed: ByteArray,
            accountIndex: Long,
            networkId: Int
        ): JniMetadataKey

        @Suppress("FunctionParameterNaming")
        @JvmStatic
        private external fun derivePrivateUseMetadataKey(
            accountMetadataKey_sk: ByteArray,
            accountMetadataKey_c: ByteArray,
            ufvk: String?,
            privateUseSubject: ByteArray,
            networkId: Int
        ): Array<ByteArray>

        @JvmStatic
        private external fun deriveArbitraryWalletKeyFromSeed(
            contextString: ByteArray,
            seed: ByteArray
        ): ByteArray

        @JvmStatic
        private external fun deriveArbitraryAccountKeyFromSeed(
            contextString: ByteArray,
            seed: ByteArray,
            accountIndex: Long,
            networkId: Int
        ): ByteArray
    }
}
