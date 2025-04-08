package cash.z.ecc.android.sdk.internal.model

internal data class RecoveryProgress(
    private val numerator: Long,
    private val denominator: Long
) {
    override fun toString() = "RecoveryProgress($numerator/$denominator) -> ${getSafeRatio()}"

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
        fun new(jni: JniWalletSummary): RecoveryProgress? =
            jni.recoveryProgressNumerator?.let { numerator ->
                jni.recoveryProgressDenominator?.let { denominator ->
                    RecoveryProgress(
                        numerator = numerator,
                        denominator = denominator
                    )
                }
            }
    }
}
