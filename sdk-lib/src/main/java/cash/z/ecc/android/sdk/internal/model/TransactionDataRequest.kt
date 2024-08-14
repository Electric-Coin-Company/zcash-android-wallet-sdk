package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.exception.SdkException
import cash.z.ecc.android.sdk.internal.ext.isInUIntRange
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 */
interface TransactionDataRequest {
    class GetStatus(val txid: ByteArray) : TransactionDataRequest

    class Enhancement(val txid: ByteArray) : TransactionDataRequest

    data class SpendsFromAddress(
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
        fun new(
            jni: JniTransactionDataRequest,
            zcashNetwork: ZcashNetwork
        ): TransactionDataRequest {
            return when (jni) {
                is JniTransactionDataRequest.GetStatus -> GetStatus(jni.txid)
                is JniTransactionDataRequest.Enhancement -> Enhancement(jni.txid)
                is JniTransactionDataRequest.SpendsFromAddress -> SpendsFromAddress(
                    jni.address,
                    BlockHeight.new(zcashNetwork, jni.startHeight),
                    if (jni.endHeight == -1L) {
                        null
                    } else {
                        BlockHeight.new(zcashNetwork, jni.endHeight)
                    }
                )

                else -> throw SdkException("Unknown JniTransactionDataRequest variant", cause = null)
            }
        }
    }
}