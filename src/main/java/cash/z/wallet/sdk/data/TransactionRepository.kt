package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.dao.WalletTransaction
import cash.z.wallet.sdk.entity.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel

interface TransactionRepository {
    fun start(parentScope: CoroutineScope)
    fun stop()
    fun balance(): ReceiveChannel<Long>
    fun allTransactions(): ReceiveChannel<List<WalletTransaction>>
    fun lastScannedHeight(): Int
    fun isInitialized(): Boolean
    suspend fun findTransactionById(txId: Long): Transaction?
    suspend fun deleteTransactionById(txId: Long)
}