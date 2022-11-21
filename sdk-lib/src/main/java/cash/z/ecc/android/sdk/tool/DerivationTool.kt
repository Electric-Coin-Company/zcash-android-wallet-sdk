package cash.z.ecc.android.sdk.tool

import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.jni.RustBackendWelding
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.type.UnifiedFullViewingKey

@Suppress("TooManyFunctions")
object DerivationTool : RustBackendWelding.Derivation {

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
            deriveUnifiedFullViewingKey(usk.copyBytes(), networkId = network.id)
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
        account: Account
    ): UnifiedSpendingKey = withRustBackendLoaded {
        deriveSpendingKey(seed, account.value, networkId = network.id)
    }

    /**
     * Given a seed and account index, return the associated Unified Address.
     *
     * @param seed the seed from which to derive the address.
     * @param accountIndex the index of the account to use for deriving the address.
     *
     * @return the address that corresponds to the seed and account index.
     */
    override suspend fun deriveUnifiedAddress(seed: ByteArray, network: ZcashNetwork, account: Account): String =
        withRustBackendLoaded {
            deriveUnifiedAddressFromSeed(seed, account.value, networkId = network.id)
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
}
