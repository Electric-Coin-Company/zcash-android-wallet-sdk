package cash.z.ecc.android.sdk.internal.jni

import cash.z.ecc.android.sdk.internal.Derivation
import cash.z.ecc.android.sdk.internal.model.JniUnifiedSpendingKey

@Suppress("TooManyFunctions")
object RustDerivationTool : Derivation {

    override suspend fun deriveUnifiedFullViewingKeys(
        seed: ByteArray,
        networkId: Int,
        numberOfAccounts: Int
    ): Array<String> =
        withRustBackendLoaded {
            deriveUnifiedFullViewingKeysFromSeed(seed, numberOfAccounts, networkId = networkId)
        }

    override suspend fun deriveUnifiedFullViewingKey(
        usk: JniUnifiedSpendingKey,
        networkId: Int
    ): String = withRustBackendLoaded {
        deriveUnifiedFullViewingKey(usk.bytes, networkId = networkId)
    }

    override suspend fun deriveUnifiedSpendingKey(
        seed: ByteArray,
        networkId: Int,
        accountIndex: Int
    ): JniUnifiedSpendingKey = withRustBackendLoaded {
        deriveSpendingKey(seed, accountIndex, networkId = networkId)
    }

    override suspend fun deriveUnifiedAddress(seed: ByteArray, networkId: Int, accountIndex: Int): String =
        withRustBackendLoaded {
            deriveUnifiedAddressFromSeed(seed, accountIndex = accountIndex, networkId = networkId)
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
        networkId: Int
    ): String = withRustBackendLoaded {
        deriveUnifiedAddressFromViewingKey(viewingKey, networkId = networkId)
    }

    /**
     * A helper function to ensure that the Rust libraries are loaded before any code in this
     * class attempts to interact with it, indirectly, by invoking JNI functions. It would be
     * nice to have an annotation like @UsesSystemLibrary for this
     */
    private suspend fun <T> withRustBackendLoaded(block: () -> T): T {
        RustBackend.loadLibrary()
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
    ): JniUnifiedSpendingKey

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
}
