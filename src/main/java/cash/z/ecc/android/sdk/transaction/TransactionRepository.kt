package cash.z.ecc.android.sdk.transaction

import androidx.paging.PagedList
import cash.z.ecc.android.sdk.db.entity.*
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
     * Find all the newly scanned transactions in the given range, including transactions (like
     * change or those only identified by nullifiers) which should not appear in the UI. This method
     * is intended for use after a scan, in order to collect all the transactions that were
     * discovered and then enhance them with additional details. It returns a list to signal that
     * the intention is not to add them to a recyclerview or otherwise show in the UI.
     *
     * @param blockHeightRange the range of blocks to check for transactions.
     *
     * @return a list of transactions that were mined in the given range, inclusive.
     */
    suspend fun findNewTransactions(blockHeightRange: IntRange): List<ConfirmedTransaction>

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
