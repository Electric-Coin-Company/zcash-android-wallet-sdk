package cash.z.wallet.sdk.transaction

import cash.z.wallet.sdk.entity.PendingTransaction
import kotlinx.coroutines.flow.Flow

/**
 * Manage outbound transactions with the main purpose of reporting which ones are still pending,
 * particularly after failed attempts or dropped connectivity. The intent is to help see outbound
 * transactions through to completion.
 */
interface OutboundTransactionManager {
    fun initSpend(
        zatoshi: Long,
        toAddress: String,
        memo: String,
        fromAccountIndex: Int
    ): Flow<PendingTransaction>
    fun encode(spendingKey: String, pendingTx: PendingTransaction): Flow<PendingTransaction>
    fun submit(pendingTx: PendingTransaction): Flow<PendingTransaction>

    /**
     * Attempt to cancel a transaction.
     *
     * @return true when the transaction was able to be cancelled.
     */
    suspend fun cancel(pendingTx: PendingTransaction): Boolean
    suspend fun getAll(): List<PendingTransaction>
}

interface TransactionError {
    val message: String
}