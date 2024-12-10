package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.internal.jni.RustBackend
import cash.z.ecc.android.sdk.internal.model.JniAccountUsk

/**
 * Account related model class providing a [ZIP 316](https://zips.z.cash/zip-0316) Unified Spending Key.
 *
 * This is the spend authority for an account under the wallet's seed.
 *
 * An instance of this class contains all of the per-pool spending keys that could be
 * derived at the time of its creation. As such, it is not suitable for long-term storage,
 * export/import, or backup purposes.
 */
class AccountUsk private constructor(
    /**
     * The account UUID used to derive this key.
     */
    val accountUuid: AccountUuid,
    /**
     * The binary encoding of the [ZIP 316](https://zips.z.cash/zip-0316) Unified Spending
     * Key for [accountUuid].
     *
     * This encoding **MUST NOT** be exposed to users. It is an internal encoding that is
     * inherently unstable, and only intended to be passed between the SDK and the storage
     * backend. Wallets **MUST NOT** allow this encoding to be exported or imported.
     */
    private val bytes: FirstClassByteArray
) {
    internal constructor(uskJni: JniAccountUsk) : this(
        AccountUuid.new(uskJni.accountUuid),
        FirstClassByteArray(uskJni.bytes.copyOf())
    )

    /**
     * The binary encoding of the [ZIP 316](https://zips.z.cash/zip-0316) Unified Spending
     * Key for [accountUuid].
     *
     * This encoding **MUST NOT** be exposed to users. It is an internal encoding that is
     * inherently unstable, and only intended to be passed between the SDK and the storage
     * backend. Wallets **MUST NOT** allow this encoding to be exported or imported.
     */
    fun copyBytes() = bytes.byteArray.copyOf()

    // Override to prevent leaking key to logs
    override fun toString() = "AccountUsk(account=$accountUuid, bytes=***)"

    companion object {
        /**
         * This method may fail if the [bytes] no longer represent a valid key.  A key could become invalid due to
         * network upgrades or other internal changes.  If a non-successful result is returned, clients are expected
         * to use [DerivationTool.deriveUnifiedSpendingKey] to regenerate the key from the seed.
         *
         * @return A validated AccountUsk.
         */
        suspend fun new(
            accountUuid: AccountUuid,
            bytes: ByteArray
        ): Result<AccountUsk> {
            val bytesCopy = bytes.copyOf()
            RustBackend.loadLibrary()
            return runCatching {
                require(RustBackend.validateUnifiedSpendingKey(bytesCopy))
                AccountUsk(accountUuid, FirstClassByteArray(bytesCopy))
            }
        }

        fun new(jniAccountUsk: JniAccountUsk): AccountUsk =
            AccountUsk(
                accountUuid = AccountUuid.new(jniAccountUsk.accountUuid),
                bytes = FirstClassByteArray(jniAccountUsk.bytes)
            )
    }
}
