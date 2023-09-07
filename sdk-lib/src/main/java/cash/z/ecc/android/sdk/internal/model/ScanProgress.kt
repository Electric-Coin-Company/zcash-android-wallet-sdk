package cash.z.ecc.android.sdk.internal.model

internal data class ScanProgress(
    val numerator: Long,
    val denominator: Long
) {
    override fun toString() = "ScanProgress($numerator/$denominator) -> ${numerator / (denominator.toFloat())}"

    companion object {
        fun new(jni: JniScanProgress): ScanProgress {
            return ScanProgress(
                numerator = jni.numerator,
                denominator = jni.denominator
            )
        }
    }
}
