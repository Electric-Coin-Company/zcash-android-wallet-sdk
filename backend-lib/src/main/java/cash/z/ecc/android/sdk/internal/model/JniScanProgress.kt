package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param numerator the numerator of the progress ratio
 * @param denominator the denominator of the progress ratio
 */
@Keep
class JniScanProgress(
    val numerator: Long,
    val denominator: Long
) {
    init {
        require(numerator >= 0L) {
            "Numerator $numerator is outside of allowed range [0, Long.MAX_VALUE]"
        }
        require(denominator >= 1L) {
            "Denominator $denominator is outside of allowed range [1, Long.MAX_VALUE]"
        }
        require(numerator.toFloat().div(denominator) >= 0f) {
            "Result of ${numerator.toFloat()}/$denominator is outside of allowed range"
        }
        require(numerator.toFloat().div(denominator) <= 1f) {
            "Result of ${numerator.toFloat()}/$denominator is outside of allowed range"
        }
    }
}
