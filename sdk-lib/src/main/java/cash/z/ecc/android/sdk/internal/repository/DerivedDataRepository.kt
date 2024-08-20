package cash.z.ecc.android.sdk.internal.repository

import cash.z.ecc.android.sdk.internal.model.DbTransactionOverview
import cash.z.ecc.android.sdk.internal.model.EncodedTransaction
import cash.z.ecc.android.sdk.internal.model.OutputProperties
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.TransactionRecipient
import kotlinx.coroutines.flow.Flow

/**
 * Repository of wallet transactions, providing an agnostic interface to the underlying information.
 */
@Suppress("TooManyFunctions")
internal interface DerivedDataRepository {
    /**
     * The height of the first transaction that hasn't been enhanced yet.
     *
     * @return the height of the first un-enhanced transaction in the repository, or null in case of all transaction
     * enhanced or no entry found
     */
    suspend fun firstUnenhancedHeight(): BlockHeight?

    /**
     * Find the encoded transaction associated with the given id.
     *
     * @param txId the id of the transaction to find.
     *
     * @return the transaction or null when it cannot be found.
     */
    suspend fun findEncodedTransactionByTxId(txId: FirstClassByteArray): EncodedTransaction?

    suspend fun getOldestTransaction(): DbTransactionOverview?

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

    val allTransactions: Flow<List<DbTransactionOverview>>

    fun getOutputProperties(transactionId: FirstClassByteArray): Flow<OutputProperties>

    fun getRecipients(transactionId: FirstClassByteArray): Flow<TransactionRecipient>

    suspend fun close()

    suspend fun isClosed(): Boolean
}
