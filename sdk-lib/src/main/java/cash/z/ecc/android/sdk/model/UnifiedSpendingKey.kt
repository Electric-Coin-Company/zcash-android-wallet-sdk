package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.jni.RustBackend

/**
 * A [ZIP 316](https://zips.z.cash/zip-0316) Unified Spending Key.
 *
 * This is the spend authority for an account under the wallet's seed.
 *
 * An instance of this class contains all of the per-pool spending keys that could be
 * derived at the time of its creation. As such, it is not suitable for long-term storage,
 * export/import, or backup purposes.
 */
data class UnifiedSpendingKey internal constructor(
    /**
     * A [ZIP 316](https://zips.z.cash/zip-0316) account identifier.
     */
    val account: Int,

    /**
     * The binary encoding of the [ZIP 316](https://zips.z.cash/zip-0316) Unified Spending
     * Key for [account].
     *
     * This encoding **MUST NOT** be exposed to users. It is an internal encoding that is
     * inherently unstable, and only intended to be passed between the SDK and the storage
     * backend. Wallets **MUST NOT** allow this encoding to be exported or imported.
     */
    internal val bytes: FirstClassByteArray
) {
    // Override to prevent leaking key to logs
    override fun toString() = "UnifiedSpendingKey(account=$account)"

    fun copyBytes() = bytes.byteArray.copyOf()

    companion object {
        suspend fun new(account: Int, bytes: ByteArray): Result<UnifiedSpendingKey> {
            val bytesCopy = bytes.copyOf()
            RustBackend.rustLibraryLoader.load()
            return Result.runCatching {
                // We can ignore the Boolean returned from this, because if an error
                // occurs the Rust side will throw.
                RustBackend.validateUnifiedSpendingKey(bytesCopy)
                return success(UnifiedSpendingKey(account, FirstClassByteArray(bytesCopy)))
            }
        }
    }
}
