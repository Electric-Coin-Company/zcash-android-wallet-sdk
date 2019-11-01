package cash.z.wallet.sdk.transaction

import android.content.Context
import androidx.paging.PagedList
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.wallet.sdk.db.BlockDao
import cash.z.wallet.sdk.db.DerivedDataDb
import cash.z.wallet.sdk.db.TransactionDao
import cash.z.wallet.sdk.entity.TransactionEntity
import cash.z.wallet.sdk.ext.ZcashSdk
import cash.z.wallet.sdk.ext.android.toFlowPagedList
import cash.z.wallet.sdk.ext.android.toRefreshable
import cash.z.wallet.sdk.ext.twig
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
            .build(),
        pageSize
    )

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

    override fun invalidate() = receivedTxDataSourceFactory.refresh()

    override fun lastScannedHeight(): Int {
        return blocks.lastScannedHeight()
    }

    override fun isInitialized(): Boolean {
        return blocks.count() > 0
    }

    override suspend fun findTransactionById(txId: Long): TransactionEntity? = withContext(IO) {
        twig("finding transaction with id $txId on thread ${Thread.currentThread().name}")
        val transaction = transactions.findById(txId)
        twig("found ${transaction?.id}")
        transaction
    }

    override suspend fun findTransactionByRawId(rawTxId: ByteArray): TransactionEntity? = withContext(IO) {
        transactions.findByRawId(rawTxId)
    }

    fun close() {
        derivedDataDb.close()
    }

}
