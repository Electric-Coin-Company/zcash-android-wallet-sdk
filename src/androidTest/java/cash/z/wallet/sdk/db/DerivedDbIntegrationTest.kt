package cash.z.wallet.sdk.db

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class DerivedDbIntegrationTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun testDbExists() {
        assertNotNull(db)
    }

    @Test
    fun testDaoExists_Transaction() {
        assertNotNull(transactions)
    }

    @Test
    fun testDaoExists_Block() {
        assertNotNull(blocks)
    }

    @Test
    fun testCount_Block() {
        assertEquals(80101, blocks.count())
    }

    @Test
    fun testNoteQuery() {
        val all = transactions.getReceivedTransactions()
        assertEquals(3, all.size)
    }

    @Test
    fun testTransactionDaoPrepopulated() {
        val tran = transactions.findById(1)

        assertEquals(343987, tran?.minedHeight)
    }

    companion object {
        private lateinit var transactions: TransactionDao
        private lateinit var blocks: BlockDao
        private lateinit var db: DerivedDataDb

        @BeforeClass
        @JvmStatic
        fun setup() {
            // TODO: put this database in the assets directory and open it from there via .openHelperFactory(new AssetSQLiteOpenHelperFactory()) seen here https://github.com/albertogiunta/sqliteAsset
            db = Room
                .databaseBuilder(ApplicationProvider.getApplicationContext(), DerivedDataDb::class.java, "new-data-glue2.db")
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .fallbackToDestructiveMigration()
                .build()
                .apply {
                    transactions = transactionDao()
                    blocks = blockDao()
                }
        }

        @AfterClass
        @JvmStatic
        fun close() {
            db.close()
        }
    }
}