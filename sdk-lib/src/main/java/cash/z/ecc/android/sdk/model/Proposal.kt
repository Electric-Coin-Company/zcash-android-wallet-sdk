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
        fun fromUnsafe(proposal: ProposalUnsafe): Proposal {
            val typed = Proposal(proposal)

            // Check for type errors eagerly, to ensure that the caller won't
            // encounter these errors later.
            typed.totalFeeRequired()

            return typed
        }
    }

    /**
     * Exposes the type-unsafe proposal variant for passing across the FFI.
     */
    fun toUnsafe(): ProposalUnsafe {
        return inner
    }

    /**
     * Returns the total fee required by this proposal for its transactions.
     */
    fun totalFeeRequired(): Zatoshi {
        return Zatoshi(inner.totalFeeRequired())
    }
}
