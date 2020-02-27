package cash.z.wallet.sdk.transaction

import androidx.paging.PagedList
import cash.z.wallet.sdk.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository of wallet transactions, providing an agnostic interface to the underlying information.
 */
interface TransactionRepository {

    /**
     * The last height scanned by this repository.
     *
     * @return the last height scanned by this repository.
     */
    fun lastScannedHeight(): Int

    /**
     * Returns true when this repository has been initialized and seeded with the initial checkpoint.
     *
     * @return true when this repository has been initialized and seeded with the initial checkpoint.
     */
    fun isInitialized(): Boolean

    /**
     * Find the encoded transaction associated with the given id.
     *
     * @param txId the id of the transaction to find.
     *
     * @return the transaction or null when it cannot be found.
     */
    suspend fun findEncodedTransactionById(txId: Long): EncodedTransaction?

    /**
     * Find the mined height that matches the given raw tx_id in bytes. This is useful for matching
     * a pending transaction with one that we've decrypted from the blockchain.
     *
     * @param rawTransactionId the id of the transaction to find.
     *
     * @return the mined height of the given transaction, if it is known to this wallet.
     */
    suspend fun findMinedHeight(rawTransactionId: ByteArray): Int?

    /**
     * Provides a way for other components to signal that the underlying data has been modified.
     */
    fun invalidate()


    //
    // Transactions
    //

    /** A flow of all the inbound confirmed transactions */
    val receivedTransactions: Flow<PagedList<ConfirmedTransaction>>
    /** A flow of all the outbound confirmed transactions */
    val sentTransactions: Flow<PagedList<ConfirmedTransaction>>
    /** A flow of all the inbound and outbound confirmed transactions */
    val allTransactions: Flow<PagedList<ConfirmedTransaction>>
}