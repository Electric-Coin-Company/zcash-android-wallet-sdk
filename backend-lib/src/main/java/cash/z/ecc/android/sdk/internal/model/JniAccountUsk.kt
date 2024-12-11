package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep
import cash.z.ecc.android.sdk.internal.jni.JNI_ACCOUNT_UUID_BYTES_SIZE

/**
 * Serves as cross layer (Kotlin, Rust) communication class. It contains account identifier together with
 * a [ZIP 316](https://zips.z.cash/zip-0316) Unified Spending Key.
 *
 * This is the spend authority for an account under the wallet's seed.
 *
 * An instance of this class contains all of the per-pool spending keys that could be
 * derived at the time of its creation. As such, it is not suitable for long-term storage,
 * export/import, or backup purposes.
 */
@Keep
class JniAccountUsk(
    /**
     * The "one-way stable" identifier for the account tracked in the wallet to which this
     * spending key belongs.
     */
    val accountUuid: ByteArray,
    /**
     * The binary encoding of the [ZIP 316](https://zips.z.cash/zip-0316) Unified Spending
     * Key for [accountUuid].
     *
     * This encoding **MUST NOT** be exposed to users. It is an internal encoding that is
     * inherently unstable, and only intended to be passed between the SDK and the storage
     * backend. Wallets **MUST NOT** allow this encoding to be exported or imported.
     */
    val bytes: ByteArray
) {
    init {
        require(accountUuid.size == JNI_ACCOUNT_UUID_BYTES_SIZE) {
            "Account UUID must be 16 bytes"
        }
    }

    // Override to prevent leaking key to logs
    override fun toString() = "JniAccountUsk(account=$accountUuid, bytes=***)"
}
