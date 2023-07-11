package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep
import co.electriccoin.lightwallet.client.model.CompactBlockUnsafe

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param startHeight the minimum height in the range (inclusive) - although it's type Long, it needs to be a UInt
 * @param endHeight the maximum height in the range (exclusive) - although it's type Long, it needs to be a UInt
 * @param priority the priority of the range for scanning
 */
@Keep
class JniScanRange(
    val startHeight: Long,
    val endHeight: Long,
    val priority: Long
) {
    init {
        // We require some of the parameters below to be in the range of unsigned integer, because of the Rust layer
        // implementation.
        require(UINT_RANGE.contains(startHeight)) {
            "Height $startHeight is outside of allowed range $UINT_RANGE"
        }
        require(UINT_RANGE.contains(endHeight)) {
            "Height $endHeight is outside of allowed range $UINT_RANGE"
        }
    }

    companion object {
        private val UINT_RANGE = 0.toLong()..UInt.MAX_VALUE.toLong()

        fun new(range: OpenEndRange<BlockHeight>, priority: Long): JniScanRange {
            return JniScanRange(
                startHeight = range.start,
                endHeight = range.endExclusive,
                priority = priority
            )
        }
    }
}
