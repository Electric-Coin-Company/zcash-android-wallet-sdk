package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep
import cash.z.ecc.android.sdk.internal.ext.isInUIntRange

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 */
@Keep
sealed class JniRewindResult {
    /**
     * A rewind was successful.
     *
     * `height` is the height to which the data store was actually truncated.
     */
    @Keep
    class Success(
        val height: Long
    ) : JniRewindResult() {
        init {
            require(height.isInUIntRange()) {
                "Height $height is outside of allowed UInt range"
            }
        }
    }

    /**
     * A requested rewind would violate invariants of the storage layer.
     *
     * If no safe rewind height can be determined, the safe rewind height member will be -1.
     */
    @Keep
    class Invalid(
        val safeRewindHeight: Long,
        val requestedHeight: Long
    ) : JniRewindResult() {
        init {
            if (safeRewindHeight != -1L) {
                require(safeRewindHeight.isInUIntRange()) {
                    "Height $safeRewindHeight is outside of allowed UInt range and is not -1"
                }
            }
            require(requestedHeight.isInUIntRange()) {
                "Height $requestedHeight is outside of allowed UInt range"
            }
        }
    }
}
