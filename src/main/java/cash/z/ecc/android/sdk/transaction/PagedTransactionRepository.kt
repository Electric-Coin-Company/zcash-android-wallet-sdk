package cash.z.ecc.android.sdk.transaction

import android.content.Context
import androidx.paging.PagedList
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.ecc.android.sdk.db.BlockDao
import cash.z.ecc.android.sdk.db.DerivedDataDb
import cash.z.ecc.android.sdk.db.TransactionDao
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.android.toFlowPagedList
import cash.z.ecc.android.sdk.ext.android.toRefreshable
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

/**
 * Example of a repository that leverages the Room paging library to return a [PagedList] of
 * transactions. Consumers can register as a page listener and receive an interface that allows for
 * efficiently paging data.
 *
 * @param pageSize transactions per page. This influences pre-fetch and memory configuration.
 */
open class PagedTransactionRepository(
    private val derivedDataDb: DerivedDataDb,
    private val pageSize: Int = 10
) : TransactionRepository {

    /**
     * Constructor that creates the database.
     */
    constructor(
        context: Context,
        pageSize: Int = 10,
        dataDbName: String = ZcashSdk.DB_DATA_NAME
    ) : this(
        Room.databaseBuilder(context, DerivedDataDb::class.java, dataDbName)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .addMigrations(DerivedDataDb.MIGRATION_3_4)
            .addMigrations(DerivedDataDb.MIGRATION_4_3)
            .addMigrations(DerivedDataDb.MIGRATION_4_5)
            .build(),
        pageSize
    )
    init {
        derivedDataDb.openHelper.writableDatabase.beginTransaction()
        derivedDataDb.openHelper.writableDatabase.endTransaction()
    }

    private val blocks: BlockDao = derivedDataDb.blockDao()
    private val transactions: TransactionDao = derivedDataDb.transactionDao()
    private val receivedTxDataSourceFactory = transactions.getReceivedTransactions().toRefreshable()
    private val sentTxDataSourceFactory = transactions.getSentTransactions().toRefreshable()
    private val allTxDataSourceFactory = transactions.getAllTransactions().toRefreshable()


    //
    // TransactionRepository API
    //

    override val receivedTransactions = receivedTxDataSourceFactory.toFlowPagedList(pageSize)
    override val sentTransactions = sentTxDataSourceFactory.toFlowPagedList(pageSize)
    override val allTransactions = allTxDataSourceFactory.toFlowPagedList(pageSize)

    override fun invalidate() = allTxDataSourceFactory.refresh()

    override fun lastScannedHeight(): Int {
        return blocks.lastScannedHeight()
    }

    override fun isInitialized(): Boolean {
        return blocks.count() > 0
    }

    override suspend fun findEncodedTransactionById(txId: Long) = withContext(IO) {
        transactions.findEncodedTransactionById(txId)
    }

    override suspend fun findNewTransactions(blockHeightRange: IntRange): List<ConfirmedTransaction> =
        transactions.findAllTransactionsByRange(blockHeightRange.first, blockHeightRange.last)


    override suspend fun findMinedHeight(rawTransactionId: ByteArray) = withContext(IO) {
        transactions.findMinedHeight(rawTransactionId)
    }


    /**
     * Close the underlying database.
     */
    fun close() {
        derivedDataDb.close()
    }

    // TODO: begin converting these into Data Access API. For now, just collect the desired operations and iterate/refactor, later
    fun findBlockHash(height: Int): ByteArray? = blocks.findHashByHeight(height)
    fun getTransactionCount(): Int = transactions.count()
}
