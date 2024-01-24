package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep
import cash.z.ecc.android.sdk.internal.ext.isInUIntRange

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param startHeight the minimum height in the scanned range (inclusive).
 *        Although it's type Long, it needs to be a UInt.
 * @param endHeight the maximum height in the scanned range (exclusive).
 *        Although it's type Long, it needs to be a UInt.
 * @param spentSaplingNoteCount the number of Sapling notes detected as spent in
 *        the scanned range.
 * @param receivedSaplingNoteCount the number of Sapling notes detected as
 *        received in the scanned range.
 * @throws IllegalArgumentException unless (startHeight and endHeight are UInts,
 *         and startHeight is not less than endHeight).
 */
@Keep
class JniScanSummary(
    val startHeight: Long,
    val endHeight: Long,
    val spentSaplingNoteCount: Long,
    val receivedSaplingNoteCount: Long
) {
    init {
        // We require some of the parameters below to be in the range of
        // unsigned integer, because they are block heights.
        require(startHeight.isInUIntRange()) {
            "Height $startHeight is outside of allowed UInt range"
        }
        require(endHeight.isInUIntRange()) {
            "Height $endHeight is outside of allowed UInt range"
        }
        require(endHeight >= startHeight) {
            "End height $endHeight must be greater than start height $startHeight."
        }
    }
}
