package cash.z.wallet.sdk.db

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import cash.z.wallet.sdk.dao.BlockDao
import cash.z.wallet.sdk.dao.CompactBlockDao
import cash.z.wallet.sdk.dao.NoteDao
import cash.z.wallet.sdk.dao.TransactionDao
import cash.z.wallet.sdk.vo.CompactBlock
import org.junit.*
import org.junit.Assert.*

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
    fun testDaoExists_Note() {
        assertNotNull(notes)
    }

    @Test
    fun testCount_Transaction() {
        assertEquals(5, transactions.count())
    }

    @Test
    fun testCount_Block() {
        assertEquals(80101, blocks.count())
    }

    @Test
    fun testCount_Note() {
        assertEquals(5, notes.count())
    }
    @Test
    fun testTransactionDaoPrepopulated() {
        val tran = transactions.findById(1)

        assertEquals(343987, tran?.block)
    }

    @Test
    fun testBlockDaoPrepopulated() {
        val tran = blocks.findById(1)?.apply {
            assertEquals(343987, this.height)
        }
    }

    companion object {
        private lateinit var transactions: TransactionDao
        private lateinit var blocks: BlockDao
        private lateinit var notes: NoteDao
        private lateinit var db: DerivedDataDb

        @BeforeClass
        @JvmStatic
        fun setup() {
            // TODO: put this database in the assets directory and open it from there via .openHelperFactory(new AssetSQLiteOpenHelperFactory()) seen here https://github.com/albertogiunta/sqliteAsset
            db = Room
                .databaseBuilder(ApplicationProvider.getApplicationContext(), DerivedDataDb::class.java, "dummy-data2.db")
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .fallbackToDestructiveMigration()
                .build()
                .apply {
                    transactions = transactionDao()
                    blocks = blockDao()
                    notes = noteDao()
                }
        }

        @AfterClass
        @JvmStatic
        fun close() {
            db.close()
        }
    }
}