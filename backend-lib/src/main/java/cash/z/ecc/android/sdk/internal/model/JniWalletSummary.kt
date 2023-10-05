package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param accountBalances the balances of the wallet accounts
 * @param progressNumerator the numerator of the progress ratio
 * @param progressDenominator the denominator of the progress ratio
 * @throws IllegalArgumentException unless (progressNumerator is nonnegative,
 *         progressDenominator is positive, and the represented ratio is in the
 *         range 0.0 to 1.0 inclusive).
 */
@Keep
class JniWalletSummary(
    val accountBalances: Array<JniAccountBalance>,
    val progressNumerator: Long,
    val progressDenominator: Long
) {
    init {
        require(progressNumerator >= 0L) {
            "Numerator $progressNumerator is outside of allowed range [0, Long.MAX_VALUE]"
        }
        require(progressDenominator >= 1L) {
            "Denominator $progressDenominator is outside of allowed range [1, Long.MAX_VALUE]"
        }
        require(progressNumerator.toFloat().div(progressDenominator) >= 0f) {
            "Result of ${progressNumerator.toFloat()}/$progressDenominator is outside of allowed range"
        }
        require(progressNumerator.toFloat().div(progressDenominator) <= 1f) {
            "Result of ${progressNumerator.toFloat()}/$progressDenominator is outside of allowed range"
        }
    }
}
