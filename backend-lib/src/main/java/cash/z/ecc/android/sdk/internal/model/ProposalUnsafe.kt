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
    fun toByteArray(): ByteArray {
        return inner.toByteArray()
    }

    /**
     * Returns the fee required by this proposal.
     */
    fun feeRequired(): Long {
        return inner.balance.feeRequired
    }
}
