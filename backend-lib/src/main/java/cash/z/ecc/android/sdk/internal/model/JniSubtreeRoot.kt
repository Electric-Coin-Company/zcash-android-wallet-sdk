package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep
import co.electriccoin.lightwallet.client.model.CompactBlockUnsafe

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param rootHash the subtree's root hash
 * @param completingBlockHeight the block height in which the subtree was completed - although it's type Long, it needs to be in UInt range
 */
@Keep
class JniSubtreeRoot(
    val rootHash: ByteArray,
    val completingBlockHeight: Long
) {
    init {
        // We require some of the parameters below to be in the range of unsigned integer,
        // because of the Rust layer implementation.
        require(UINT_RANGE.contains(completingBlockHeight)) {
            "Height $completingBlockHeight is outside of allowed range $UINT_RANGE"
        }
    }

    companion object {
        private val UINT_RANGE = 0.toLong()..UInt.MAX_VALUE.toLong()

        fun new(subtreeRoot: SubtreeRootUnsafe): JniSubtreeRoot {
            return JniSubtreeRoot(
                rootHash = subtreeRoot.rootHash,
                completingBlockHeight = subtreeRoot.completingBlockHeight.toLong()
            )
        }
    }
}
