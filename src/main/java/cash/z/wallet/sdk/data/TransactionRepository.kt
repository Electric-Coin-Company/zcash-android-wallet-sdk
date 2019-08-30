package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.entity.ClearedTransaction
import cash.z.wallet.sdk.entity.Transaction

interface TransactionRepository {
    fun lastScannedHeight(): Int
    fun isInitialized(): Boolean
    suspend fun findTransactionById(txId: Long): Transaction?
    suspend fun findTransactionByRawId(rawTransactionId: ByteArray): Transaction?
    suspend fun deleteTransactionById(txId: Long)
    suspend fun getClearedTransactions(): List<ClearedTransaction>
    suspend fun monitorChanges(listener: () -> Unit)
}