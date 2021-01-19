package cash.z.ecc.android.sdk.tool

import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.jni.RustBackendWelding

class DerivationTool {

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
        override fun deriveViewingKeys(seed: ByteArray, numberOfAccounts: Int): Array<String> =
            withRustBackendLoaded {
                deriveExtendedFullViewingKeys(seed, numberOfAccounts)
            }

        /**
         * Given a spending key, return the associated viewing key.
         *
         * @param spendingKey the key from which to derive the viewing key.
         *
         * @return the viewing key that corresponds to the spending key.
         */
        override fun deriveViewingKey(spendingKey: String): String = withRustBackendLoaded {
            deriveExtendedFullViewingKey(spendingKey)
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
        override fun deriveSpendingKeys(seed: ByteArray, numberOfAccounts: Int): Array<String> =
            withRustBackendLoaded {
                deriveExtendedSpendingKeys(seed, numberOfAccounts)
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
        override fun deriveShieldedAddress(seed: ByteArray, accountIndex: Int): String =
            withRustBackendLoaded {
                deriveShieldedAddressFromSeed(seed, accountIndex)
            }

        /**
         * Given a viewing key string, return the associated address.
         *
         * @param viewingKey the viewing key to use for deriving the address. The viewing key is tied to
         * a specific account so no account index is required.
         *
         * @return the address that corresponds to the viewing key.
         */
        override fun deriveShieldedAddress(viewingKey: String): String = withRustBackendLoaded {
            deriveShieldedAddressFromViewingKey(viewingKey)
        }

        // WIP probably shouldn't be used just yet. Why?
        //  - because we need the private key associated with this seed and this function doesn't return it.
        //  - the underlying implementation needs to be split out into a few lower-level calls
        override fun deriveTransparentAddress(seed: ByteArray): String = withRustBackendLoaded {
            deriveTransparentAddressFromSeed(seed)
        }

        fun validateViewingKey(viewingKey: String) {
            // TODO
        }

        /**
         * A helper function to ensure that the Rust libraries are loaded before any code in this
         * class attempts to interact with it, indirectly, by invoking JNI functions. It would be
         * nice to have an annotation like @UsesSystemLibrary for this
         */
        private fun <T> withRustBackendLoaded(block: () -> T): T {
            RustBackend.load()
            return block()
        }

        //
        // JNI functions
        //

        @JvmStatic
        private external fun deriveExtendedSpendingKeys(
            seed: ByteArray,
            numberOfAccounts: Int
        ): Array<String>

        @JvmStatic
        private external fun deriveExtendedFullViewingKeys(
            seed: ByteArray,
            numberOfAccounts: Int
        ): Array<String>

        @JvmStatic
        private external fun deriveExtendedFullViewingKey(spendingKey: String): String

        @JvmStatic
        private external fun deriveShieldedAddressFromSeed(
            seed: ByteArray,
            accountIndex: Int
        ): String

        @JvmStatic
        private external fun deriveShieldedAddressFromViewingKey(key: String): String

        @JvmStatic
        private external fun deriveTransparentAddressFromSeed(seed: ByteArray): String
    }
}
