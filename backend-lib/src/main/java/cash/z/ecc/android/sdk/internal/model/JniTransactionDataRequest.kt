package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep
import cash.z.ecc.android.sdk.internal.ext.isInUIntRange

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 */
@Keep
interface JniTransactionDataRequest {
    @Keep
    class GetStatus(val txid: ByteArray) : JniTransactionDataRequest
    @Keep
    class Enhancement(val txid: ByteArray) : JniTransactionDataRequest
    @Keep
    data class SpendsFromAddress(
        val address: String,
        val startHeight: Long,
        val endHeight: Long,
    ) : JniTransactionDataRequest {
        init {
            // We require some of the parameters below to be in the range of unsigned integer, because of the Rust layer
            // implementation.
            require(startHeight.isInUIntRange()) {
                "Height $startHeight is outside of allowed UInt range"
            }
            // We use -1L to represent None across JNI.
            if (endHeight != -1L) {
                require(endHeight.isInUIntRange()) {
                    "Height $endHeight is outside of allowed UInt range"
                }
                require(endHeight >= startHeight) {
                    "End height $endHeight must be greater than start height $startHeight."
                }
            }
        }
    }
}
