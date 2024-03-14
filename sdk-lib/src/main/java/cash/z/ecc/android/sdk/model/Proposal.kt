package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.internal.model.ProposalUnsafe

/**
 * A transaction proposal created by the Rust backend in response to a Kotlin request.
 *
 * @param inner the type-unsafe Proposal protobuf received across the FFI.
 */
class Proposal(
    private val inner: ProposalUnsafe
) {
    companion object {
        /**
         * @throws IllegalArgumentException if the proposal is invalid.
         */
        @Throws(IllegalArgumentException::class)
        fun fromUnsafe(proposal: ProposalUnsafe) =
            Proposal(proposal).also {
                it.check()
            }

        /**
         * @throws IllegalArgumentException if the given [ByteArray] data could not be parsed and mapped to the new
         * type-safe Proposal class.
         */
        @Throws(IllegalArgumentException::class)
        fun fromByteArray(array: ByteArray) = fromUnsafe(ProposalUnsafe.parse(array))
    }

    // Check for type errors eagerly, to ensure that the caller won't encounter these errors later.
    private fun check() {
        totalFeeRequired()
    }

    /**
     * Exposes the type-unsafe proposal variant for passing across the FFI.
     */
    fun toUnsafe(): ProposalUnsafe {
        return inner
    }

    /**
     * Serializes this proposal type-safe data to [ByteArray] for storing purposes.
     */
    fun toByteArray(): ByteArray {
        return inner.toByteArray()
    }

    /**
     * Returns the number of transactions that this proposal will create.
     *
     * This is equal to the number of `TransactionSubmitResult`s that will be returned
     * from `Synchronizer.createProposedTransactions`.
     *
     * Proposals always create at least one transaction.
     */
    fun transactionCount(): Int {
        return inner.transactionCount()
    }

    /**
     * Returns the total fee required by this proposal for its transactions.
     */
    fun totalFeeRequired(): Zatoshi {
        return Zatoshi(inner.totalFeeRequired())
    }

    fun toPrettyString(): String {
        return "Transaction count: ${transactionCount()}, Total fee required: ${totalFeeRequired()}"
    }
}
