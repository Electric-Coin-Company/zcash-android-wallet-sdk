package cash.z.ecc.android.sdk.tool

import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.jni.RustBackendWelding
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.type.UnifiedViewingKey

class DerivationTool {

    @Suppress("TooManyFunctions")
    companion object : RustBackendWelding.Derivation {

        /**
         * Given a seed and a number of accounts, return the associated viewing keys.
         *
         * @param seed the seed from which to derive viewing keys.
         * @param numberOfAccounts the number of accounts to use. Multiple accounts are not fully
         * supported so the default value of 1 is recommended.
         *
         * @return the viewing keys that correspond to the seed, formatted as Strings.
         */
        override suspend fun deriveUnifiedViewingKeys(seed: ByteArray, network: ZcashNetwork, numberOfAccounts: Int): Array<UnifiedViewingKey> =
            withRustBackendLoaded {
                deriveUnifiedViewingKeysFromSeed(seed, numberOfAccounts, networkId = network.id).map {
                    UnifiedViewingKey(it[0], it[1])
                }.toTypedArray()
            }

        /**
         * Given a spending key, return the associated viewing key.
         *
         * @param spendingKey the key from which to derive the viewing key.
         *
         * @return the viewing key that corresponds to the spending key.
         */
        override suspend fun deriveViewingKey(spendingKey: String, network: ZcashNetwork): String = withRustBackendLoaded {
            deriveExtendedFullViewingKey(spendingKey, networkId = network.id)
        }

        /**
         * Given a seed and a number of accounts, return the associated spending keys.
         *
         * @param seed the seed from which to derive spending keys.
         * @param numberOfAccounts the number of accounts to use. Multiple accounts are not fully
         * supported so the default value of 1 is recommended.
         *
         * @return the spending keys that correspond to the seed, formatted as Strings.
         */
        override suspend fun deriveSpendingKeys(seed: ByteArray, network: ZcashNetwork, numberOfAccounts: Int): Array<String> =
            withRustBackendLoaded {
                deriveExtendedSpendingKeys(seed, numberOfAccounts, networkId = network.id)
            }

        /**
         * Given a seed and account index, return the associated address.
         *
         * @param seed the seed from which to derive the address.
         * @param accountIndex the index of the account to use for deriving the address. Multiple
         * accounts are not fully supported so the default value of 1 is recommended.
         *
         * @return the address that corresponds to the seed and account index.
         */
        override suspend fun deriveShieldedAddress(seed: ByteArray, network: ZcashNetwork, accountIndex: Int): String =
            withRustBackendLoaded {
                deriveShieldedAddressFromSeed(seed, accountIndex, networkId = network.id)
            }

        /**
         * Given a viewing key string, return the associated address.
         *
         * @param viewingKey the viewing key to use for deriving the address. The viewing key is tied to
         * a specific account so no account index is required.
         *
         * @return the address that corresponds to the viewing key.
         */
        override suspend fun deriveShieldedAddress(viewingKey: String, network: ZcashNetwork): String = withRustBackendLoaded {
            deriveShieldedAddressFromViewingKey(viewingKey, networkId = network.id)
        }

        // WIP probably shouldn't be used just yet. Why?
        //  - because we need the private key associated with this seed and this function doesn't return it.
        //  - the underlying implementation needs to be split out into a few lower-level calls
        override suspend fun deriveTransparentAddress(seed: ByteArray, network: ZcashNetwork, account: Int, index: Int): String = withRustBackendLoaded {
            deriveTransparentAddressFromSeed(seed, account, index, networkId = network.id)
        }

        override suspend fun deriveTransparentAddressFromPublicKey(publicKey: String, network: ZcashNetwork): String = withRustBackendLoaded {
            deriveTransparentAddressFromPubKey(pk = publicKey, networkId = network.id)
        }

        override suspend fun deriveTransparentAddressFromPrivateKey(privateKey: String, network: ZcashNetwork): String = withRustBackendLoaded {
            deriveTransparentAddressFromPrivKey(sk = privateKey, networkId = network.id)
        }

        override suspend fun deriveTransparentSecretKey(seed: ByteArray, network: ZcashNetwork, account: Int, index: Int): String = withRustBackendLoaded {
            deriveTransparentSecretKeyFromSeed(seed, account, index, networkId = network.id)
        }

        @Suppress("UNUSED_PARAMETER")
        fun validateUnifiedViewingKey(viewingKey: UnifiedViewingKey, networkId: Int = ZcashNetwork.Mainnet.id) {
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
        private external fun deriveExtendedSpendingKeys(
            seed: ByteArray,
            numberOfAccounts: Int,
            networkId: Int
        ): Array<String>

        @JvmStatic
        private external fun deriveUnifiedViewingKeysFromSeed(
            seed: ByteArray,
            numberOfAccounts: Int,
            networkId: Int
        ): Array<Array<String>>

        @JvmStatic
        private external fun deriveExtendedFullViewingKey(spendingKey: String, networkId: Int): String

        @JvmStatic
        private external fun deriveShieldedAddressFromSeed(
            seed: ByteArray,
            accountIndex: Int,
            networkId: Int
        ): String

        @JvmStatic
        private external fun deriveShieldedAddressFromViewingKey(key: String, networkId: Int): String

        @JvmStatic
        private external fun deriveTransparentAddressFromSeed(seed: ByteArray, account: Int, index: Int, networkId: Int): String

        @JvmStatic
        private external fun deriveTransparentAddressFromPubKey(pk: String, networkId: Int): String

        @JvmStatic
        private external fun deriveTransparentAddressFromPrivKey(sk: String, networkId: Int): String

        @JvmStatic
        private external fun deriveTransparentSecretKeyFromSeed(seed: ByteArray, account: Int, index: Int, networkId: Int): String
    }
}
