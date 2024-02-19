package cash.z.ecc.android.sdk.model

/**
 * A result object for a transaction that was created as part of a proposal, indicating
 * whether it was submitted to the network or if an error occurred.
 */
sealed class TransactionSubmitResult {
    /**
     * The transaction was successfully submitted to the mempool.
     */
    data class Success(val txId: FirstClassByteArray) : TransactionSubmitResult()

    /**
     * An error occurred while submitting the transaction.
     *
     * If `grpcError` is true, the transaction failed to reach the `lightwalletd` server.
     * Otherwise, the transaction reached the `lightwalletd` server but failed to enter
     * the mempool.
     */
    data class Failure(
        val txId: FirstClassByteArray,
        val grpcError: Boolean,
        val code: Int,
        val description: String?
    ) : TransactionSubmitResult()

    /**
     * The transaction was created and is in the local wallet, but was not submitted to
     * the network.
     */
    data class NotAttempted(val txId: FirstClassByteArray) : TransactionSubmitResult()
}
