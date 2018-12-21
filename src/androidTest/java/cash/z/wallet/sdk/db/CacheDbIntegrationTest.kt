package cash.z.wallet.sdk.db

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import cash.z.wallet.sdk.dao.CompactBlockDao
import cash.z.wallet.sdk.vo.CompactBlock
import org.junit.*
import org.junit.Assert.*

class CacheDbIntegrationTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun testDbExists() {
        assertNotNull(db)
    }

    @Test
    fun testDaoExists() {
        assertNotNull(dao)
    }

    @Test
    fun testDaoPrepopulated() {
        dao.findById(343900).apply {
            assertEquals(343900, this?.height)
        }
    }

    companion object {
        private lateinit var dao: CompactBlockDao
        private lateinit var db: CompactBlockDb

        @BeforeClass
        @JvmStatic
        fun setup() {
            // TODO: put this database in the assets directory and open it from there via .openHelperFactory(new AssetSQLiteOpenHelperFactory()) seen here https://github.com/albertogiunta/sqliteAsset
            db = Room
                .databaseBuilder(ApplicationProvider.getApplicationContext(), CompactBlockDb::class.java, "dummy-cache.db")
//                .databaseBuilder(ApplicationProvider.getApplicationContext(), CompactBlockDb::class.java, "compact-blocks.db")
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .fallbackToDestructiveMigration()
                .build()
                .apply { dao = complactBlockDao() }
        }

        @AfterClass
        @JvmStatic
        fun close() {
            db.close()
        }
    }
}