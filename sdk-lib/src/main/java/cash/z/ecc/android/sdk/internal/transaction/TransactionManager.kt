package cash.z.ecc.android.sdk.internal.transaction

import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.Zatoshi
import kotlinx.coroutines.flow.Flow

/**
 * Manage outbound transactions with the main purpose of reporting which ones are still pending,
 * particularly after failed attempts or dropped connectivity. The intent is to help see outbound
 * transactions through to completion.
 */
interface OutboundTransactionManager {
    /**
     * Initialize a spend with the main purpose of creating an idea to use for tracking it until
     * completion.
     *
     * @param zatoshi the amount to spend.
     * @param toAddress the address to which funds will be sent.
     * @param memo the optionally blank memo associated with this transaction.
     * @param fromAccountIndex the account from which to spend funds.
     *
     * @return the associated pending transaction whose ID can be used to monitor for changes.
     */
    suspend fun initSpend(
        zatoshi: Zatoshi,
        toAddress: String,
        memo: String,
        fromAccountIndex: Int
    ): PendingTransaction

    /**
     * Encode the pending transaction using the given spending key. This is a local operation that
     * produces a raw transaction to submit to lightwalletd.
     *
     * @param spendingKey the spendingKey to use for constructing the transaction.
     * @param pendingTx the transaction information created by [initSpend] that will be used to
     * construct a transaction.
     *
     * @return the resulting pending transaction whose ID can be used to monitor for changes.
     */
    suspend fun encode(spendingKey: String, pendingTx: PendingTransaction): PendingTransaction

    suspend fun encode(
        spendingKey: String,
        transparentSecretKey: String,
        pendingTx: PendingTransaction
    ): PendingTransaction

    /**
     * Submits the transaction represented by [pendingTx] to lightwalletd to broadcast to the
     * network and, hopefully, include in the next block.
     *
     * @param pendingTx the transaction information containing the raw bytes that will be submitted
     * to lightwalletd.
     *
     * @return the resulting pending transaction whose ID can be used to monitor for changes.
     */
    suspend fun submit(pendingTx: PendingTransaction): PendingTransaction

    /**
     * Given a transaction and the height at which it was mined, update the transaction to indicate
     * that it was mined.
     *
     * @param pendingTx the pending transaction that has been mined.
     * @param minedHeight the height at which the given transaction was mined, according to the data
     * that has been processed from the blockchain.
     */
    suspend fun applyMinedHeight(pendingTx: PendingTransaction, minedHeight: BlockHeight)

    /**
     * Generate a flow of information about the given id where a new pending transaction is emitted
     * every time its state changes.
     *
     * @param id the id to monitor.
     *
     * @return a flow of pending transactions that are emitted anytime the transaction associated
     * with the given id changes.
     */
    suspend fun monitorById(id: Long): Flow<PendingTransaction>

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
     * Attempt to cancel a transaction.
     *
     * @param pendingTx the transaction matching the ID of the transaction to cancel.
     *
     * @return true when the transaction was able to be cancelled.
     */
    suspend fun cancel(pendingId: Long): Boolean

    /**
     * Delete the given transaction but return 0 if it did not exist.
     *
     * @param transaction the transaction to be processed.
     *
     * @return the total number of transactions successfully removed from storage.
     */
    suspend fun abort(transaction: PendingTransaction): Int

    /**
     * Get all pending transactions known to this wallet as a flow that is updated anytime the list
     * changes.
     *
     * @return a flow of all pending transactions known to this wallet.
     */
    fun getAll(): Flow<List<PendingTransaction>>

    // this is mostly useful for tests we can restrict it to tests if we need to
    suspend fun findById(id: Long): PendingTransaction?

    suspend fun markForDeletion(id: Long)
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
