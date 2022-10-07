package cash.z.ecc.android.sdk.internal.db.derived

import cash.z.ecc.android.sdk.internal.model.EncodedTransaction
import cash.z.ecc.android.sdk.internal.repository.DerivedDataRepository
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

@Suppress("TooManyFunctions")
internal class DbDerivedDataRepository(
    private val derivedDataDb: DerivedDataDb
) : DerivedDataRepository {
    override suspend fun lastScannedHeight(): BlockHeight {
        return derivedDataDb.blockTable.lastScannedHeight()
    }

    override suspend fun firstScannedHeight(): BlockHeight {
        return derivedDataDb.blockTable.firstScannedHeight()
    }

    override suspend fun isInitialized(): Boolean {
        return derivedDataDb.blockTable.count() > 0
    }

    override suspend fun findEncodedTransactionById(txId: Long): EncodedTransaction? {
        return derivedDataDb.transactionTable.findEncodedTransactionById(txId)
    }

    override suspend fun findNewTransactions(blockHeightRange: ClosedRange<BlockHeight>): List<Transaction> =
        derivedDataDb.allTransactionView.getTransactionRange(blockHeightRange).toList()

    override suspend fun findMinedHeight(rawTransactionId: ByteArray) = derivedDataDb.transactionTable
        .findMinedHeight(rawTransactionId)

    override suspend fun findMatchingTransactionId(rawTransactionId: ByteArray) = derivedDataDb.transactionTable
        .findDatabaseId(rawTransactionId)

    override suspend fun findBlockHash(height: BlockHeight) = derivedDataDb.blockTable.findBlockHash(height)

    override suspend fun getTransactionCount() = derivedDataDb.transactionTable.count()

    override fun invalidate() {
        TODO("Not yet implemented")
    }

    override suspend fun cleanupCancelledTx(rawTransactionId: ByteArray): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteExpired(lastScannedHeight: BlockHeight): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getAccount(accountId: Int) = derivedDataDb.accountTable.getAccount(accountId)

    override suspend fun getAccountCount() = derivedDataDb.accountTable.count()
        // toInt() should be safe because we expect very few accounts
        .toInt()

    override val receivedTransactions: Flow<Transaction.Received>
        get() = derivedDataDb.receivedTransactionView.getReceivedTransactions()
    override val sentTransactions: Flow<Transaction.Sent>
        get() = derivedDataDb.sentTransactionView.getSentTransactions()

    override val allTransactions: Flow<Transaction>
        get() = derivedDataDb.allTransactionView.getAllTransactions()

    override suspend fun close() {
        derivedDataDb.close()
    }
}
