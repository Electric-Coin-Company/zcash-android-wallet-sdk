package cash.z.wallet.sdk.transaction

import cash.z.wallet.sdk.entity.PendingTransaction
import kotlinx.coroutines.flow.Flow

/**
 * Manage outbound transactions with the main purpose of reporting which ones are still pending,
 * particularly after failed attempts or dropped connectivity. The intent is to help see outbound
 * transactions through to completion.
 */
interface OutboundTransactionManager {
    suspend fun initSpend(
        zatoshi: Long,
        toAddress: String,
        memo: String,
        fromAccountIndex: Int
    ): PendingTransaction
    suspend fun encode(spendingKey: String, pendingTx: PendingTransaction): PendingTransaction
    suspend fun submit(pendingTx: PendingTransaction): PendingTransaction
    suspend fun applyMinedHeight(pendingTx: PendingTransaction, minedHeight: Int)
    suspend fun monitorById(id: Long): Flow<PendingTransaction>

    suspend fun isValidShieldedAddress(address: String): Boolean
    suspend fun isValidTransparentAddress(address: String): Boolean

    /**
     * Attempt to cancel a transaction.
     *
     * @return true when the transaction was able to be cancelled.
     */
    suspend fun cancel(pendingTx: PendingTransaction): Boolean
    fun getAll(): Flow<List<PendingTransaction>>
}

interface TransactionError {
    val message: String
}