package cash.z.ecc.android.sdk.internal.model

internal data class ScanProgress(
    private val numerator: Long,
    private val denominator: Long
) {
    override fun toString() = "ScanProgress($numerator/$denominator) -> ${getSafeRatio()}"

    /**
     * Returns progress ratio in [0, 1] range. Any out-of-range value is treated as 0.
     */
    fun getSafeRatio() =
        numerator.toFloat().div(denominator).let { ratio ->
            if (ratio < 0f || ratio > 1f) {
                0f
            } else {
                ratio
            }
        }

    companion object {
        fun new(jni: JniWalletSummary): ScanProgress =
            ScanProgress(
                numerator = jni.progressNumerator,
                denominator = jni.progressDenominator
            )
    }
}
