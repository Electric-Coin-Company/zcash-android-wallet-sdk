package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 */
@Keep
sealed class JniAddressCheckResult {
    @Keep
    data object NotFound : JniAddressCheckResult()

    @Keep
    class Found(
        val address: String
    ) : JniAddressCheckResult()
}
