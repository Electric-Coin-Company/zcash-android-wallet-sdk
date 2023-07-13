package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork

@OptIn(ExperimentalStdlibApi::class)
internal data class ScanRange(
    val range: OpenEndRange<BlockHeight>,
    val priority: Long
) {
    override fun toString() = "ScanRange(range=$range, priority=$priority)"

    companion object {
        fun new(jni: JniScanRange, zcashNetwork: ZcashNetwork): ScanRange {
            return ScanRange(
                range = BlockHeight.new(zcashNetwork, jni.startHeight)..<BlockHeight.new(zcashNetwork, jni.endHeight),
                priority = jni.priority
            )
        }
    }
}
