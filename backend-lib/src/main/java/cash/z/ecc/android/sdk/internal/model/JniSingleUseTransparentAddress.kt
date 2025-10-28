package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param address the ephemeral address.
 * @param gapPosition The position of the ephemeral address within its gap.
 * @param gapLimit The maximum number of ephemeral addresses that may be produced without
 *        receiving a transaction on one of them.
 *
 * @throws IllegalArgumentException if the values are inconsistent.
 */
@Keep
class JniSingleUseTransparentAddress(
    val address: String,
    val gapPosition: Int,
    val gapLimit: Int,
) {
    init {
        require(gapPosition >= 0) {
            "Gap position must be non-negative"
        }
        require(gapLimit >= 0) {
            "Gap limit must be non-negative"
        }
        require(gapPosition < gapLimit) {
            "Gap position must be less than the gap limit"
        }
    }
}
