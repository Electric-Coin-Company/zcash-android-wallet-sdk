package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep
import cash.z.ecc.android.sdk.internal.ext.isInUIntRange

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 */
@Keep
sealed class JniTransactionDataRequest {
    @Keep
    class GetStatus(
        val txid: ByteArray
    ) : JniTransactionDataRequest()

    @Keep
    class Enhancement(
        val txid: ByteArray
    ) : JniTransactionDataRequest()

    @Keep
    data class TransactionsInvolvingAddress(
        val address: String,
        val startHeight: Long,
        val endHeight: Long,
        val requestAt: Long,
        val txStatusFilter: JniTransactionStatusFilter,
        val outputStatusFilter: JniOutputStatusFilter,
    ) : JniTransactionDataRequest() {
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
            // We use -1L to represent None across JNI.
            if (requestAt != -1L) {
                require(requestAt >= 0) {
                    "requestAt $requestAt is outside of allowed timestamp range"
                }
            }
        }
    }
}

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 */
@Keep
sealed class JniTransactionStatusFilter {
    @Keep
    data object Mined : JniTransactionStatusFilter()

    @Keep
    data object Mempool : JniTransactionStatusFilter()

    @Keep
    data object All : JniTransactionStatusFilter()
}

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 */
@Keep
sealed class JniOutputStatusFilter {
    @Keep
    data object Unspent : JniOutputStatusFilter()

    @Keep
    data object All : JniOutputStatusFilter()
}
