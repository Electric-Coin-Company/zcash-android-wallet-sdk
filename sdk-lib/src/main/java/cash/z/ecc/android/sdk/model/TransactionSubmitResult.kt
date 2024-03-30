package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.internal.ext.toHexReversed

/**
 * A result object for a transaction that was created as part of a proposal, indicating
 * whether it was submitted to the network or if an error occurred.
 */
sealed class TransactionSubmitResult(
    open val txId: FirstClassByteArray
) {
    /**
     * @return Transaction ID correctly transformed into String representation
     */
    fun txIdString() = txId.byteArray.toHexReversed()

    /**
     * The transaction was successfully submitted to the mempool.
     */
    data class Success(override val txId: FirstClassByteArray) : TransactionSubmitResult(txId)

    /**
     * An error occurred while submitting the transaction.
     *
     * If `grpcError` is true, the transaction failed to reach the `lightwalletd` server.
     * Otherwise, the transaction reached the `lightwalletd` server but failed to enter
     * the mempool.
     */
    data class Failure(
        override val txId: FirstClassByteArray,
        val grpcError: Boolean,
        val code: Int,
        val description: String?
    ) : TransactionSubmitResult(txId)

    /**
     * The transaction was created and is in the local wallet, but was not submitted to
     * the network.
     */
    data class NotAttempted(override val txId: FirstClassByteArray) : TransactionSubmitResult(txId)
}
