package cash.z.ecc.android.sdk.internal.repository

import cash.z.ecc.android.sdk.internal.model.EncodedTransaction
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.Transaction
import cash.z.ecc.android.sdk.model.TransactionOverview
import kotlinx.coroutines.flow.Flow

/**
 * Repository of wallet transactions, providing an agnostic interface to the underlying information.
 */
@Suppress("TooManyFunctions")
internal interface DerivedDataRepository {

    /**
     * The last height scanned by this repository.
     *
     * @return the last height scanned by this repository.
     */
    suspend fun lastScannedHeight(): BlockHeight

    /**
     * The height of the first block in this repository. This is typically the checkpoint that was
     * used to initialize this wallet. If we overwrite this block, it breaks our ability to spend
     * funds.
     */
    suspend fun firstScannedHeight(): BlockHeight

    /**
     * @return true when this repository has been initialized and seeded with the initial checkpoint.
     */
    suspend fun isInitialized(): Boolean

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
    suspend fun findNewTransactions(blockHeightRange: ClosedRange<BlockHeight>): List<TransactionOverview>

    suspend fun getOldestTransaction(): TransactionOverview?

    /**
     * Find the mined height that matches the given raw tx_id in bytes. This is useful for matching
     * a pending transaction with one that we've decrypted from the blockchain.
     *
     * @param rawTransactionId the id of the transaction to find.
     *
     * @return the mined height of the given transaction, if it is known to this wallet.
     */
    suspend fun findMinedHeight(rawTransactionId: ByteArray): BlockHeight?

    suspend fun findMatchingTransactionId(rawTransactionId: ByteArray): Long?

    // TODO [#681]: begin converting these into Data Access API. For now, just collect the desired
    //  operations and iterate/refactor, later
    // TODO [#681]: https://github.com/zcash/zcash-android-wallet-sdk/issues/681
    suspend fun findBlockHash(height: BlockHeight): ByteArray?

    suspend fun getTransactionCount(): Long

    /**
     * Provides a way for other components to signal that the underlying data has been modified.
     */
    fun invalidate()

    suspend fun getAccountCount(): Int

    //
    // Transactions
    //

    /*
     * Note there are two big limitations with this implementation:
     *  1. Clients don't receive notification if the underlying data changes.  A flow of flows could help there.
     *  2. Pagination isn't supported.  Although flow does a good job of allowing the data to be processed as a stream,
     *     that doesn't work so well in UI when users might scroll forwards/backwards.
     *
     * We'll come back to this and improve it in the future.  This implementation is already an improvement over
     * prior versions.
     */

    /** A flow of all the inbound confirmed transactions */
    val receivedTransactions: Flow<List<Transaction.Received>>

    /** A flow of all the outbound confirmed transactions */
    val sentTransactions: Flow<List<Transaction.Sent>>

    /** A flow of all the inbound and outbound confirmed transactions */
    val allTransactions: Flow<List<TransactionOverview>>

    fun getSentNoteIds(transactionId: Long): Flow<Long>

    fun getReceivedNoteIds(transactionId: Long): Flow<Long>

    suspend fun close()
}
