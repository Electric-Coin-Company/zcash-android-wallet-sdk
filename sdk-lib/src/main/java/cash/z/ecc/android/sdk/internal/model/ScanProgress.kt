package cash.z.ecc.android.sdk.internal.model

internal data class ScanProgress(
    private val numerator: Long,
    private val denominator: Long
) {
    override fun toString() = "ScanProgress($numerator/$denominator) -> ${getSafeRatio()}"

    /**
     * Returns progress ratio in [0, 1] range. Any out-of-range value is treated as 0.
     */
    fun getSafeRatio() = numerator.toFloat().div(denominator).let { ration ->
        if (ration < 0f || ration > 1f) {
            0f
        } else {
            ration
        }
    }

    companion object {
        fun new(jni: JniScanProgress): ScanProgress {
            return ScanProgress(
                numerator = jni.numerator,
                denominator = jni.denominator
            )
        }
    }
}
