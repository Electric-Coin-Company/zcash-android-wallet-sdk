package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep
import cash.z.ecc.android.sdk.internal.ext.isInUIntRange

/**
 * High-level information about the output of a transaction received by the
 * wallet.
 *
 * This type is capable of representing both shielded and transparent outputs.
 * It does not internally store the transaction ID, so it must be interpreted in
 * the context of a caller having requested output information for a specific
 * transaction.
 *
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param poolType The pool in which the output was received.
 * @param outputIndex The index of the output among the transaction's outputs to
 *        the associated pool.
 * @param value The value of the output.
 * @param confirmationsUntilSpendable The number of confirmations required for
 *        the output to be treated as spendable,.
 * @throws IllegalArgumentException if the values are inconsistent.
 */
@Keep
@Suppress("LongParameterList")
class JniReceivedTransactionOutput(
    val poolType: Int,
    val outputIndex: Int,
    val value: Long,
    val confirmationsUntilSpendable: Long,
) {
    init {
        require(poolType >= 0) {
            "Pool type $poolType must be non-negative"
        }
        require(outputIndex >= 0) {
            "Output index $outputIndex must be non-negative"
        }
        require(value >= 0L) {
            "Value $value must be non-negative"
        }
        require(confirmationsUntilSpendable.isInUIntRange()) {
            "Confirmations until spendable $confirmationsUntilSpendable must be a UInt"
        }
    }
}
