package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork

internal data class ScanRange(
    val range: ClosedRange<BlockHeight>,
    val priority: Long
) {
    override fun toString() = "ScanRange(range=$range, priority=${getSuggestScanRangePriority()})"

    internal fun getSuggestScanRangePriority(): SuggestScanRangePriority {
        Twig.verbose { "Current suggested scan range priority: $priority" }
        return SuggestScanRangePriority.values()
            .firstOrNull { it.priority == priority } ?: SuggestScanRangePriority.Scanned
    }

    companion object {
        /**
         *  Note that this function subtracts 1 from [JniScanRange.endHeight] as the rest of the logic works with
         *  [ClosedRange] and the endHeight is exclusive.
         */
        fun new(jni: JniScanRange, zcashNetwork: ZcashNetwork): ScanRange {
            return ScanRange(
                range = BlockHeight.new(zcashNetwork, jni.startHeight)..
                    (BlockHeight.new(zcashNetwork, jni.endHeight) - 1),
                priority = jni.priority
            )
        }
    }
}

@Suppress("MagicNumber")
internal enum class SuggestScanRangePriority(val priority: Long) {
    Scanned(10),
    Historic(20),
    OpenAdjacent(30),
    FoundNote(40),
    ChainTip(50),
    Verify(60)
}
