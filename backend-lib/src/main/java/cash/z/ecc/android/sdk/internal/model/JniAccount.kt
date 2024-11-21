package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param account the account ID
 * @param ufvk The account's Unified Full Viewing Key, if any.
 * @throws IllegalArgumentException if the values are inconsistent.
 */
@Keep
@Suppress("LongParameterList")
class JniAccount(
    val accountId: Long,
    val ufvk: String?,
) {
    init {
        require(accountId >= 0) {
            "Account ID must be non-negative"
        }
    }
}
