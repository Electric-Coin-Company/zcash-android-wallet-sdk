package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep
import co.electriccoin.lightwallet.client.model.CompactBlockUnsafe

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param height the block's height - although it's type Long, it needs to be in UInt range
 * @param hash the block's hash (ID of the block)
 * @param time the block's time. Unix epoch time when the block was mined.
 * @param saplingOutputsCount the sapling outputs count - although its type is Long, it needs to be in UInt range
 * @param orchardOutputsCount the orchard outputs count - although its type is Long, it needs to be in UInt range
 */
@Keep
class JniBlockMeta(
    val height: Long,
    val hash: ByteArray,
    val time: Long,
    val saplingOutputsCount: Long,
    val orchardOutputsCount: Long
) {
    init {
        // We require some of the parameters below to be in the range of unsigned integer, because of the Rust layer
        // implementation.
        require(UINT_RANGE.contains(height)) {
            "Height $height is outside of allowed range $UINT_RANGE"
        }
        require(UINT_RANGE.contains(saplingOutputsCount)) {
            "SaplingOutputsCount $saplingOutputsCount is outside of allowed range $UINT_RANGE"
        }
        require(UINT_RANGE.contains(orchardOutputsCount)) {
            "SaplingOutputsCount $orchardOutputsCount is outside of allowed range $UINT_RANGE"
        }
    }

    companion object {
        private val UINT_RANGE = 0.toLong()..UInt.MAX_VALUE.toLong()

        internal fun new(block: CompactBlockUnsafe): JniBlockMeta {
            return JniBlockMeta(
                height = block.height,
                hash = block.hash,
                time = block.time.toLong(),
                saplingOutputsCount = block.saplingOutputsCount.toLong(),
                orchardOutputsCount = block.orchardOutputsCount.toLong()
            )
        }
    }
}
