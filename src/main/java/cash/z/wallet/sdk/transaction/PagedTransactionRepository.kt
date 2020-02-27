package cash.z.wallet.sdk.transaction

import android.content.Context
import androidx.paging.PagedList
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.wallet.sdk.db.BlockDao
import cash.z.wallet.sdk.db.DerivedDataDb
import cash.z.wallet.sdk.db.TransactionDao
import cash.z.wallet.sdk.ext.ZcashSdk
import cash.z.wallet.sdk.ext.android.toFlowPagedList
import cash.z.wallet.sdk.ext.android.toRefreshable
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
            .addMigrations(MIGRATION_4_3)
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

    override fun invalidate() = receivedTxDataSourceFactory.refresh()

    override fun lastScannedHeight(): Int {
        return blocks.lastScannedHeight()
    }

    override fun isInitialized(): Boolean {
        return blocks.count() > 0
    }

    override suspend fun findEncodedTransactionById(txId: Long) = withContext(IO) {
        transactions.findEncodedTransactionById(txId)
    }

    override suspend fun findMinedHeight(rawTransactionId: ByteArray) = withContext(IO) {
        transactions.findMinedHeight(rawTransactionId)
    }

    /**
     * Close the underlying database.
     */
    fun close() {
        derivedDataDb.close()
    }


    //
    // Migrations
    //

    companion object {
//        val MIGRATION_3_4 = object : Migration(3, 4) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                database.execSQL("PRAGMA foreign_keys = OFF;")
//                database.execSQL("""
//                    CREATE TABLE IF NOT EXISTS received_notes_new (
//                        id_note INTEGER PRIMARY KEY, tx INTEGER NOT NULL,
//                        output_index INTEGER NOT NULL, account INTEGER NOT NULL,
//                        diversifier BLOB NOT NULL, value INTEGER NOT NULL,
//                        rcm BLOB NOT NULL, nf BLOB NOT NULL UNIQUE,
//                        is_change INTEGER NOT NULL, memo BLOB,
//                        spent INTEGER
//                    ); """.trimIndent()
//                )
//                database.execSQL("INSERT INTO received_notes_new SELECT * FROM received_notes;")
//                database.execSQL("DROP TABLE received_notes;")
//                database.execSQL("ALTER TABLE received_notes_new RENAME TO received_notes;")
//                database.execSQL("PRAGMA foreign_keys = ON;")
//            }
//        }

        private val MIGRATION_4_3 = object : Migration(4, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("PRAGMA foreign_keys = OFF;")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS received_notes_new (
                        id_note INTEGER PRIMARY KEY,
                        tx INTEGER NOT NULL,
                        output_index INTEGER NOT NULL,
                        account INTEGER NOT NULL,
                        diversifier BLOB NOT NULL,
                        value INTEGER NOT NULL,
                        rcm BLOB NOT NULL,
                        nf BLOB NOT NULL UNIQUE,
                        is_change INTEGER NOT NULL,
                        memo BLOB,
                        spent INTEGER,
                        FOREIGN KEY (tx) REFERENCES transactions(id_tx),
                        FOREIGN KEY (account) REFERENCES accounts(account),
                        FOREIGN KEY (spent) REFERENCES transactions(id_tx),
                        CONSTRAINT tx_output UNIQUE (tx, output_index)
                    ); """.trimIndent()
                )
                database.execSQL("INSERT INTO received_notes_new SELECT * FROM received_notes;")
                database.execSQL("DROP TABLE received_notes;")
                database.execSQL("ALTER TABLE received_notes_new RENAME TO received_notes;")
                database.execSQL("PRAGMA foreign_keys = ON;")
            }
        }
    }

}