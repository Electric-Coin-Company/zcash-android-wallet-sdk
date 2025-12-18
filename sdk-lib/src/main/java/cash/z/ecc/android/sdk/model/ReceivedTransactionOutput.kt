package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.internal.model.JniReceivedTransactionOutput
import cash.z.ecc.android.sdk.internal.model.ZcashProtocol

/**
 * High-level information about the output of a transaction received by the
 * wallet.
 *
 * This type is capable of representing both shielded and transparent outputs.
 * It does not internally store the transaction ID, so it must be interpreted in
 * the context of a caller having requested output information for a specific
 * transaction.
 *
 * @param protocol The pool in which the output was received.
 * @param outputIndex The index of the output among the transaction's outputs to
 *        the associated pool.
 * @param value The value of the output.
 * @param confirmationsUntilSpendable The number of confirmations required for
 *        the output to be treated as spendable,.
 * @throws IllegalArgumentException if the values are inconsistent.
 */
data class ReceivedTransactionOutput internal constructor(
    val protocol: ZcashProtocol,
    val outputIndex: Int,
    val value: Zatoshi,
    val confirmationsUntilSpendable: UInt,
) {
    companion object {
        fun new(jniOutput: JniReceivedTransactionOutput): ReceivedTransactionOutput =
            ReceivedTransactionOutput(
                protocol = ZcashProtocol.fromPoolType(jniOutput.poolType),
                outputIndex = jniOutput.outputIndex,
                value = Zatoshi(jniOutput.value),
                confirmationsUntilSpendable = jniOutput.confirmationsUntilSpendable.toUInt(),
            )
    }
}
