package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep

/**
 * A [ZIP 316](https://zips.z.cash/zip-0316) Unified Spending Key.
 *
 * This is the spend authority for an account under the wallet's seed.
 *
 * An instance of this class contains all of the per-pool spending keys that could be
 * derived at the time of its creation. As such, it is not suitable for long-term storage,
 * export/import, or backup purposes.
 */
@Keep
class JniUnifiedSpendingKey(
    /**
     * The [ZIP 32](https://zips.z.cash/zip-0032) account index used to derive this key.
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
    val bytes: ByteArray
) {
    init {
        require(accountUuid.size == 16) {
            "Account UUID must be 16 bytes"
        }
    }

    // Override to prevent leaking key to logs
    override fun toString() = "JniUnifiedSpendingKey(account=$account, bytes=***)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JniUnifiedSpendingKey

        if (account != other.account) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = account.hashCode()
        result = 31 * result + bytes.hashCode()
        return result
    }
}
