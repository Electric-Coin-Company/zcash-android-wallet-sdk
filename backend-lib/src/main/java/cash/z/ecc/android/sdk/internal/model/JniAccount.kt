package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param accountIndex the ZIP 32 account index.
 * @param ufvk The account's Unified Full Viewing Key, if any.
 * @throws IllegalArgumentException if the values are inconsistent.
 */
@Keep
@Suppress("LongParameterList")
class JniAccount(
    val accountIndex: Int,
    val ufvk: String?,
) {
    init {
        require(accountIndex >= 0) {
            "Account index must be non-negative"
        }
    }
}
