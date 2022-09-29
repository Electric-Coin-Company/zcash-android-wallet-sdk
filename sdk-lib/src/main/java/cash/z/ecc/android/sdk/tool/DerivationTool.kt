package cash.z.ecc.android.sdk.tool

import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.jni.RustBackendWelding
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.type.UnifiedFullViewingKey

@Suppress("UtilityClassWithPublicConstructor")
class DerivationTool {

    @Suppress("TooManyFunctions")
    companion object : RustBackendWelding.Derivation {

        /**
         * Given a seed and a number of accounts, return the associated Unified Full Viewing Keys.
         *
         * @param seed the seed from which to derive viewing keys.
         * @param numberOfAccounts the number of accounts to use. Multiple accounts are not fully
         * supported so the default value of 1 is recommended.
         *
         * @return the UFVKs derived from the seed, encoded as Strings.
         */
        override suspend fun deriveUnifiedFullViewingKeys(
            seed: ByteArray,
            network: ZcashNetwork,
            numberOfAccounts: Int
        ): Array<UnifiedFullViewingKey> =
            withRustBackendLoaded {
                deriveUnifiedFullViewingKeysFromSeed(seed, numberOfAccounts, networkId = network.id).map {
                    UnifiedFullViewingKey(it)
                }.toTypedArray()
            }

        /**
         * Given a unified spending key, return the associated unified full viewing key.
         *
         * @param usk the key from which to derive the viewing key.
         *
         * @return a unified full viewing key.
         */
        override suspend fun deriveUnifiedFullViewingKey(
            usk: UnifiedSpendingKey,
            network: ZcashNetwork
        ): UnifiedFullViewingKey = withRustBackendLoaded {
            UnifiedFullViewingKey(
                deriveUnifiedFullViewingKey(usk.bytes.byteArray, networkId = network.id)
            )
        }

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
        override suspend fun deriveUnifiedSpendingKey(
            seed: ByteArray,
            network: ZcashNetwork,
            account: Int
        ): UnifiedSpendingKey = withRustBackendLoaded {
            deriveSpendingKey(seed, account, networkId = network.id)
        }

        /**
         * Given a seed and account index, return the associated Unified Address.
         *
         * @param seed the seed from which to derive the address.
         * @param accountIndex the index of the account to use for deriving the address. Multiple
         * accounts are not fully supported so the default value of 1 is recommended.
         *
         * @return the address that corresponds to the seed and account index.
         */
        override suspend fun deriveUnifiedAddress(seed: ByteArray, network: ZcashNetwork, accountIndex: Int): String =
            withRustBackendLoaded {
                deriveUnifiedAddressFromSeed(seed, accountIndex, networkId = network.id)
            }

        /**
         * Given a Unified Full Viewing Key string, return the associated Unified Address.
         *
         * @param viewingKey the viewing key to use for deriving the address. The viewing key is tied to
         * a specific account so no account index is required.
         *
         * @return the address that corresponds to the viewing key.
         */
        override suspend fun deriveUnifiedAddress(
            viewingKey: String,
            network: ZcashNetwork
        ): String = withRustBackendLoaded {
            deriveUnifiedAddressFromViewingKey(viewingKey, networkId = network.id)
        }

        // WIP probably shouldn't be used just yet. Why?
        //  - because we need the private key associated with this seed and this function doesn't return it.
        //  - the underlying implementation needs to be split out into a few lower-level calls
        override suspend fun deriveTransparentAddress(
            seed: ByteArray,
            network: ZcashNetwork,
            account: Int,
            index: Int
        ): String = withRustBackendLoaded {
            deriveTransparentAddressFromSeed(seed, account, index, networkId = network.id)
        }

        override suspend fun deriveTransparentAddressFromPublicKey(
            publicKey: String,
            network: ZcashNetwork
        ): String = withRustBackendLoaded {
            deriveTransparentAddressFromPubKey(pk = publicKey, networkId = network.id)
        }

        override suspend fun deriveTransparentAddressFromAccountPrivateKey(
            privateKey: String,
            network: ZcashNetwork,
            index: Int
        ): String = withRustBackendLoaded {
            deriveTransparentAddressFromAccountPrivKey(sk = privateKey, index = index, networkId = network.id)
        }

        override suspend fun deriveTransparentAccountPrivateKey(
            seed: ByteArray,
            network: ZcashNetwork,
            account: Int
        ): String = withRustBackendLoaded {
            deriveTransparentAccountPrivKeyFromSeed(seed, account, networkId = network.id)
        }

        @Suppress("UNUSED_PARAMETER")
        fun validateUnifiedFullViewingKey(viewingKey: UnifiedFullViewingKey, networkId: Int = ZcashNetwork.Mainnet.id) {
            // TODO [#654] https://github.com/zcash/zcash-android-wallet-sdk/issues/654
        }

        /**
         * A helper function to ensure that the Rust libraries are loaded before any code in this
         * class attempts to interact with it, indirectly, by invoking JNI functions. It would be
         * nice to have an annotation like @UsesSystemLibrary for this
         */
        private suspend fun <T> withRustBackendLoaded(block: () -> T): T {
            RustBackend.rustLibraryLoader.load()
            return block()
        }

        //
        // JNI functions
        //

        @JvmStatic
        private external fun deriveSpendingKey(
            seed: ByteArray,
            account: Int,
            networkId: Int
        ): UnifiedSpendingKey

        @JvmStatic
        private external fun deriveUnifiedFullViewingKeysFromSeed(
            seed: ByteArray,
            numberOfAccounts: Int,
            networkId: Int
        ): Array<String>

        @JvmStatic
        private external fun deriveUnifiedFullViewingKey(usk: ByteArray, networkId: Int): String

        @JvmStatic
        private external fun deriveUnifiedAddressFromSeed(
            seed: ByteArray,
            accountIndex: Int,
            networkId: Int
        ): String

        @JvmStatic
        private external fun deriveUnifiedAddressFromViewingKey(key: String, networkId: Int): String

        @JvmStatic
        private external fun deriveTransparentAddressFromSeed(
            seed: ByteArray,
            account: Int,
            index: Int,
            networkId: Int
        ): String

        @JvmStatic
        private external fun deriveTransparentAddressFromPubKey(pk: String, networkId: Int): String

        @JvmStatic
        private external fun deriveTransparentAddressFromAccountPrivKey(sk: String, index: Int, networkId: Int): String

        @JvmStatic
        private external fun deriveTransparentAccountPrivKeyFromSeed(
            seed: ByteArray,
            account: Int,
            networkId: Int
        ): String
    }
}
