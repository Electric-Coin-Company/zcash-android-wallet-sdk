package cash.z.ecc.android.sdk.internal.db.derived

import cash.z.ecc.android.sdk.internal.repository.DerivedDataRepository
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ConfirmedTransaction
import cash.z.ecc.android.sdk.model.EncodedTransaction
import cash.z.ecc.android.sdk.type.UnifiedAddressAccount
import kotlinx.coroutines.flow.Flow

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

    override suspend fun findNewTransactions(blockHeightRange: ClosedRange<BlockHeight>): List<ConfirmedTransaction> {
        TODO("Not yet implemented")
    }

    override suspend fun findMinedHeight(rawTransactionId: ByteArray): BlockHeight? {
        TODO("Not yet implemented")
    }

    override suspend fun findMatchingTransactionId(rawTransactionId: ByteArray): Long? {
        TODO("Not yet implemented")
    }

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

    override suspend fun count(): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getAccount(accountId: Int): UnifiedAddressAccount? {
        TODO("Not yet implemented")
    }

    override suspend fun getAccountCount(): Int {
        TODO("Not yet implemented")
    }

    override val receivedTransactions: Flow<List<ConfirmedTransaction>>
        get() = TODO("Not yet implemented")
    override val sentTransactions: Flow<List<ConfirmedTransaction>>
        get() = TODO("Not yet implemented")
    override val allTransactions: Flow<List<ConfirmedTransaction>>
        get() = TODO("Not yet implemented")

    override suspend fun close() {
        derivedDataDb.close()
    }
}
