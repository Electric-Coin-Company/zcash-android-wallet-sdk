package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param accountUuid the "one-way stable" identifier for the account.
 * @param ufvk The account's Unified Full Viewing Key, if any.
 * @throws IllegalArgumentException if the values are inconsistent.
 */
@Keep
class JniAccount(
    val accountUuid: ByteArray,
    val ufvk: String?,
) {
    init {
        require(accountUuid.size == 16) {
            "Account UUID must be 16 bytes"
        }
    }
}
