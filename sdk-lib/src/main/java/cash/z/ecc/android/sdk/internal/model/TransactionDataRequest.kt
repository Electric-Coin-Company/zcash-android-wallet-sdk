package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.exception.SdkException
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.TransactionId

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 */
sealed interface TransactionDataRequest {
    sealed interface Enhanceable : TransactionDataRequest {
        val txid: TransactionId

        fun txIdString(): String? = txid.txIdString()
    }

    data class GetStatus(
        override val txid: TransactionId
    ) : Enhanceable

    data class Enhancement(
        override val txid: TransactionId
    ) : Enhanceable

    data class SpendFromAddress(
        val address: String,
        val startHeight: BlockHeight,
        val endHeight: BlockHeight?,
    ) : TransactionDataRequest {
        init {
            if (endHeight != null) {
                require(endHeight >= startHeight) {
                    "End height $endHeight must not be less than start height $startHeight."
                }
            }
        }
    }

    companion object {
        fun new(jni: JniTransactionDataRequest): TransactionDataRequest =
            when (jni) {
                is JniTransactionDataRequest.GetStatus -> GetStatus(TransactionId.new(jni.txid))
                is JniTransactionDataRequest.Enhancement -> Enhancement(TransactionId.new(jni.txid))
                is JniTransactionDataRequest.SpendsFromAddress ->
                    SpendFromAddress(
                        address = jni.address,
                        startHeight = BlockHeight.new(jni.startHeight),
                        endHeight = if (jni.endHeight == -1L) null else BlockHeight.new(jni.endHeight)
                    )

                else -> throw SdkException("Unknown JniTransactionDataRequest variant", cause = null)
            }
    }
}
