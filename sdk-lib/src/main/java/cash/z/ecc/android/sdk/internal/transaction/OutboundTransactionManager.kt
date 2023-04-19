package cash.z.ecc.android.sdk.internal.transaction

import cash.z.ecc.android.sdk.internal.model.EncodedTransaction
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.TransactionRecipient
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
     * Submits the transaction represented by [encodedTransaction] to lightwalletd to broadcast to the
     * network and, hopefully, include in the next block.
     *
     * @param encodedTransaction the transaction information containing the raw bytes that will be submitted
     * to lightwalletd.
     * @return true if the transaction was successfully submitted to lightwalletd.
     */
    suspend fun submit(encodedTransaction: EncodedTransaction): Boolean

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
