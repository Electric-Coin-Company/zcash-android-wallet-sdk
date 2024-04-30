package cash.z.ecc.android.sdk.internal.db.derived

import cash.z.ecc.android.sdk.internal.model.DbTransactionOverview
import cash.z.ecc.android.sdk.internal.model.EncodedTransaction
import cash.z.ecc.android.sdk.internal.repository.DerivedDataRepository
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.TransactionRecipient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import java.util.UUID

@Suppress("TooManyFunctions")
internal class DbDerivedDataRepository(
    private val derivedDataDb: DerivedDataDb
) : DerivedDataRepository {
    private val invalidatingFlow = MutableStateFlow(UUID.randomUUID())

    override suspend fun firstUnenhancedHeight(): BlockHeight? {
        return derivedDataDb.allTransactionView.firstUnenhancedHeight()
    }

    override suspend fun findEncodedTransactionByTxId(txId: FirstClassByteArray): EncodedTransaction? {
        return derivedDataDb.transactionTable.findEncodedTransactionByTxId(txId)
    }

    override suspend fun findNewTransactions(blockHeightRange: ClosedRange<BlockHeight>): List<DbTransactionOverview> =
        derivedDataDb.allTransactionView.getTransactionRange(blockHeightRange).toList()

    override suspend fun getOldestTransaction() = derivedDataDb.allTransactionView.getOldestTransaction()

    override suspend fun findMinedHeight(rawTransactionId: ByteArray) =
        derivedDataDb.transactionTable
            .findMinedHeight(rawTransactionId)

    override suspend fun findMatchingTransactionId(rawTransactionId: ByteArray) =
        derivedDataDb.transactionTable
            .findDatabaseId(rawTransactionId)

    override suspend fun getTransactionCount() = derivedDataDb.transactionTable.count()

    override fun invalidate() {
        invalidatingFlow.value = UUID.randomUUID()
    }

    override suspend fun getAccountCount() =
        derivedDataDb.accountTable.count()
            // toInt() should be safe because we expect very few accounts
            .toInt()

    override val allTransactions: Flow<List<DbTransactionOverview>>
        get() = invalidatingFlow.map { derivedDataDb.allTransactionView.getAllTransactions().toList() }

    override fun getOutputProperties(transactionId: FirstClassByteArray) =
        derivedDataDb.txOutputsView
            .getOutputProperties(transactionId)

    override fun getRecipients(transactionId: FirstClassByteArray): Flow<TransactionRecipient> {
        return derivedDataDb.txOutputsView.getRecipients(transactionId)
    }

    override suspend fun close() {
        derivedDataDb.close()
    }

    override suspend fun isClosed(): Boolean {
        return derivedDataDb.isClosed()
    }
}
