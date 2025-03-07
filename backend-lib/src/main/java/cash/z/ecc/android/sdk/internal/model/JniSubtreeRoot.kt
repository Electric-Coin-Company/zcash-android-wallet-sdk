package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep
import cash.z.ecc.android.sdk.internal.ext.isInUIntRange

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param rootHash the subtree's root hash
 * @param completingBlockHeight the block height in which the subtree was completed - although it's type Long, it needs
 * to be in UInt range
 */
@Keep
class JniSubtreeRoot(
    val rootHash: ByteArray,
    val completingBlockHeight: Long
) {
    init {
        // We require some of the parameters below to be in the range of unsigned integer,
        // because of the Rust layer implementation.
        require(completingBlockHeight.isInUIntRange()) {
            "Height $completingBlockHeight is outside of allowed UInt range"
        }
    }

    companion object {
        fun new(
            rootHash: ByteArray,
            completingBlockHeight: Long
        ): JniSubtreeRoot =
            JniSubtreeRoot(
                rootHash = rootHash,
                completingBlockHeight = completingBlockHeight
            )
    }
}
