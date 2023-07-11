package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.BlockHeight

internal data class ScanRange(
    val range: OpenEndRange<BlockHeight>,
    val priority: Long
) {
    override fun toString() = "ScanRange(range=$range, priority=$priority)"

    companion object {
        fun new(jni: JniScanRange): ScanRange {
            return ScanRange(
                range = jni.startHeight..jni.endHeight,
                priority = priority
            )
        }
    }
}
