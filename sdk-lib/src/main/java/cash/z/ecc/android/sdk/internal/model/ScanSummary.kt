package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork

internal data class ScanSummary(
    val scannedRange: ClosedRange<BlockHeight>,
    val spentSaplingNoteCount: Long,
    val receivedSaplingNoteCount: Long
) {
    companion object {
        /**
         *  Note that this function subtracts 1 from [JniScanSummary.endHeight]
         *  as the rest of the logic works with [ClosedRange] and the endHeight
         *  is exclusive.
         */
        fun new(
            jni: JniScanSummary,
            zcashNetwork: ZcashNetwork
        ): ScanSummary {
            return ScanSummary(
                scannedRange =
                    BlockHeight.new(
                        zcashNetwork,
                        jni.startHeight
                    )..(
                        BlockHeight.new(
                            zcashNetwork,
                            jni.endHeight
                        ) - 1
                    ),
                spentSaplingNoteCount = jni.spentSaplingNoteCount,
                receivedSaplingNoteCount = jni.receivedSaplingNoteCount
            )
        }
    }
}
