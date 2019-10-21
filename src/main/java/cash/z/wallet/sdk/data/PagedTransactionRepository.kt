package cash.z.wallet.sdk.data

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.paging.toLiveData
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.wallet.sdk.db.BlockDao
import cash.z.wallet.sdk.db.DerivedDataDb
import cash.z.wallet.sdk.db.TransactionDao
import cash.z.wallet.sdk.entity.ClearedTransaction
import cash.z.wallet.sdk.entity.Transaction
import cash.z.wallet.sdk.ext.ZcashSdk
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
    private val transactionLivePagedList =
        transactions.getReceivedTransactions().toLiveData(pageSize)

    /**
     * The primary function of this repository. Callers to this method receive a [PagedList]
     * snapshot of the current data source that can then be queried by page, via the normal [List]
     * API. Meaning, the details of the paging behavior, including caching and pre-fetch are handled
     * automatically. This integrates directly with the RecyclerView for seamless display of a large
     * number of transactions.
     */
    fun setTransactionPageListener(
        lifecycleOwner: LifecycleOwner,
        listener: (PagedList<out ClearedTransaction>) -> Unit
    ) {
        transactionLivePagedList.removeObservers(lifecycleOwner)
        transactionLivePagedList.observe(lifecycleOwner, Observer { transactions ->
            listener(transactions)
        })
    }

    override fun lastScannedHeight(): Int {
        return blocks.lastScannedHeight()
    }

    override fun isInitialized(): Boolean {
        return blocks.count() > 0
    }

    override suspend fun findTransactionById(txId: Long): Transaction? = withContext(IO) {
        twig("finding transaction with id $txId on thread ${Thread.currentThread().name}")
        val transaction = transactions.findById(txId)
        twig("found ${transaction?.id}")
        transaction
    }

    override suspend fun findTransactionByRawId(rawTxId: ByteArray): Transaction? = withContext(IO) {
        transactions.findByRawId(rawTxId)
    }

    override suspend fun deleteTransactionById(txId: Long) = withContext(IO) {
        twigTask("deleting transaction with id $txId") {
            transactions.deleteById(txId)
        }
    }

    fun close() {
        derivedDataDb.close()
    }

}
