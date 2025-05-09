package cash.z.ecc.android.sdk.internal.model

import cash.z.wallet.sdk.internal.ffi.ProposalOuterClass.FeeRule
import cash.z.wallet.sdk.internal.ffi.ProposalOuterClass.Proposal

/**
 * A transaction proposal created by the Rust backend in response to a Kotlin request.
 *
 * @param inner the parsed Proposal protobuf received across the FFI.
 */
class ProposalUnsafe(
    private val inner: Proposal
) {
    init {
        require(inner.feeRule != FeeRule.FeeRuleNotSpecified) {
            "Fee rule must be specified"
        }
    }

    companion object {
        /**
         * Parses a Proposal protobuf received across the FFI.
         *
         * @throws com.google.protobuf.InvalidProtocolBufferException
         */
        @Throws(com.google.protobuf.InvalidProtocolBufferException::class)
        fun parse(encoded: ByteArray): ProposalUnsafe {
            val inner = Proposal.parseFrom(encoded)
            return ProposalUnsafe(inner)
        }
    }

    /**
     * Serializes this proposal for passing back across the FFI.
     */
    fun toByteArray(): ByteArray = inner.toByteArray()

    /**
     * Returns the number of transactions that this proposal will create.
     *
     * This is equal to the number of `TransactionSubmitResult`s that will be returned
     * from `Synchronizer.createProposedTransactions`.
     *
     * Proposals always create at least one transaction.
     */
    fun transactionCount(): Int = inner.stepsCount

    /**
     * Returns the total fee required by this proposal for its transactions.
     */
    fun totalFeeRequired(): Long = inner.stepsList.fold(0) { acc, step -> acc + step.balance.feeRequired }
}
