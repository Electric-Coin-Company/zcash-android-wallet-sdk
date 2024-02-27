package cash.z.ecc.android.sdk.internal.transaction

import cash.z.ecc.android.sdk.internal.model.EncodedTransaction
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.Proposal
import cash.z.ecc.android.sdk.model.TransactionRecipient
import cash.z.ecc.android.sdk.model.TransactionSubmitResult
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.Zatoshi

/**
 * Manage outbound transactions with the main purpose of reporting which ones are still pending,
 * particularly after failed attempts or dropped connectivity. The intent is to help see outbound
 * transactions through to completion.
 */
@Suppress("TooManyFunctions")
internal interface OutboundTransactionManager {
    /**
     * Encode the pending transaction using the given spending key. This is a local operation that
     * produces a raw transaction to submit to lightwalletd.
     *
     * @param usk the unified spending key to use for constructing the transaction.
     * @param amount the amount to send.
     * @param recipient the recipient of the transaction.
     * @param memo the memo to include in the transaction.
     * @param account the account to use for the transaction.
     *
     * @return The encoded transaction, which can be submitted to lightwalletd.
     */
    suspend fun encode(
        usk: UnifiedSpendingKey,
        amount: Zatoshi,
        recipient: TransactionRecipient,
        memo: String,
        account: Account
    ): EncodedTransaction

    /**
     * Creates a proposal for transferring funds to the given recipient.
     *
     * @param account the account from which to transfer funds.
     * @param recipient the recipient's address.
     * @param amount the amount of zatoshi to send.
     * @param memo the optional memo to include as part of the proposal's transactions.
     *
     * @return the proposal or an exception
     */
    suspend fun proposeTransfer(
        account: Account,
        recipient: String,
        amount: Zatoshi,
        memo: String
    ): Proposal

    /**
     * Creates a proposal for shielding any transparent funds received by the given account.
     *
     * @param account the account for which to shield funds.
     * @param shieldingThreshold the minimum transparent balance required before a
     *                           proposal will be created.
     * @param memo the optional memo to include as part of the proposal's transactions.
     */
    suspend fun proposeShielding(
        account: Account,
        shieldingThreshold: Zatoshi,
        memo: String
    ): Proposal

    /**
     * Creates the transactions in the given proposal.
     *
     * @param proposal the proposal for which to create transactions.
     * @param usk the unified spending key associated with the account for which the
     *            proposal was created.
     *
     * @return the successfully encoded transactions or an exception
     */
    suspend fun createProposedTransactions(
        proposal: Proposal,
        usk: UnifiedSpendingKey
    ): List<EncodedTransaction>

    /**
     * Submits the transaction represented by [encodedTransaction] to lightwalletd to broadcast to the
     * network and, hopefully, include in the next block.
     *
     * @param encodedTransaction the transaction information containing the raw bytes that will be submitted
     * to lightwalletd.
     * @return true if the transaction was successfully submitted to lightwalletd.
     */
    suspend fun submit(encodedTransaction: EncodedTransaction): TransactionSubmitResult

    /**
     * Return true when the given address is a valid t-addr.
     *
     * @param address the address to validate.
     *
     * @return true when the given address is a valid t-addr.
     */
    suspend fun isValidShieldedAddress(address: String): Boolean

    /**
     * Return true when the given address is a valid z-addr.
     *
     * @param address the address to validate.
     *
     * @return true when the given address is a valid z-addr.
     */
    suspend fun isValidTransparentAddress(address: String): Boolean

    /**
     * Return true when the given address is a valid ZIP 316 Unified Address.
     *
     * @param address the address to validate.
     *
     * @return true when the given address is a valid ZIP 316 Unified Address.
     */
    suspend fun isValidUnifiedAddress(address: String): Boolean
}

/**
 * Interface for transaction errors.
 */
interface TransactionError {
    /**
     * The message associated with this error.
     */
    val message: String
}
