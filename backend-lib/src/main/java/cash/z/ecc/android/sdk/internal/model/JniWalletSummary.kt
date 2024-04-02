package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep
import cash.z.ecc.android.sdk.internal.ext.isInUIntRange

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param accountBalances the balances of the wallet accounts
 * @param chainTipHeight the wallet's view of the current chain tip
 * @param fullyScannedHeight the height below which all blocks have been scanned
 *        by the wallet, ignoring blocks below the wallet birthday.
 * @param progressNumerator the numerator of the progress ratio
 * @param progressDenominator the denominator of the progress ratio
 * @param nextSaplingSubtreeIndex the Sapling subtree index that should start
 *        the next range of subtree roots passed to `Backend.putSaplingSubtreeRoots`.
 * @param nextOrchardSubtreeIndex the Orchard subtree index that should start
 *        the next range of subtree roots passed to `Backend.putOrchardSubtreeRoots`.
 * @throws IllegalArgumentException unless (progressNumerator is nonnegative,
 *         progressDenominator is positive, and the represented ratio is in the
 *         range 0.0 to 1.0 inclusive).
 */
@Keep
@Suppress("LongParameterList")
class JniWalletSummary(
    val accountBalances: Array<JniAccountBalance>,
    val chainTipHeight: Long,
    val fullyScannedHeight: Long,
    val progressNumerator: Long,
    val progressDenominator: Long,
    val nextSaplingSubtreeIndex: Long,
    val nextOrchardSubtreeIndex: Long,
) {
    init {
        require(chainTipHeight.isInUIntRange()) {
            "Height $chainTipHeight is outside of allowed UInt range"
        }
        require(fullyScannedHeight.isInUIntRange()) {
            "Height $fullyScannedHeight is outside of allowed UInt range"
        }
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
        require(nextSaplingSubtreeIndex.isInUIntRange()) {
            "Height $nextSaplingSubtreeIndex is outside of allowed UInt range"
        }
        require(nextOrchardSubtreeIndex.isInUIntRange()) {
            "Height $nextOrchardSubtreeIndex is outside of allowed UInt range"
        }
    }
}
