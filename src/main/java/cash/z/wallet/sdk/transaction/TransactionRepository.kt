package cash.z.wallet.sdk.transaction

import androidx.paging.PagedList
import cash.z.wallet.sdk.entity.*
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun lastScannedHeight(): Int
    fun isInitialized(): Boolean
    suspend fun findTransactionById(txId: Long): TransactionEntity?
    suspend fun findTransactionByRawId(rawTransactionId: ByteArray): TransactionEntity?

    /**
     * Provides a way for other components to signal that the underlying data has been modified.
     */
    fun invalidate()


    //
    // Transactions
    //

    val receivedTransactions: Flow<PagedList<ConfirmedTransaction>>
    val sentTransactions: Flow<PagedList<ConfirmedTransaction>>
    val allTransactions: Flow<PagedList<ConfirmedTransaction>>
}