package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.BlockHeight

internal data class ScanRange(
    val range: ClosedRange<BlockHeight>,
    val priority: Long
) {
    override fun toString() = "ScanRange(range=$range, priority=${getSuggestScanRangePriority()})"

    internal fun getSuggestScanRangePriority(): SuggestScanRangePriority {
        return SuggestScanRangePriority.entries.first { it.priority == priority }
    }

    init {
        require(SuggestScanRangePriority.entries.map { it.priority }.contains(priority)) {
            "Unsupported priority $priority used"
        }
    }

    companion object {
        /**
         *  Note that this function subtracts 1 from [JniScanRange.endHeight] as the rest of the logic works with
         *  [ClosedRange] and the endHeight is exclusive.
         */
        fun new(jni: JniScanRange): ScanRange {
            return ScanRange(
                range = BlockHeight.new(jni.startHeight)..(BlockHeight.new(jni.endHeight) - 1),
                priority = jni.priority
            )
        }
    }
}

@Suppress("MagicNumber")
internal enum class SuggestScanRangePriority(val priority: Long) {
    Ignored(0),
    Scanned(10),
    Historic(20),
    OpenAdjacent(30),
    FoundNote(40),
    ChainTip(50),
    Verify(60)
}
