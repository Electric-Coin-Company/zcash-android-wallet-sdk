package cash.z.wallet.sdk.db

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cash.z.wallet.sdk.dao.CompactBlockDao
import cash.z.wallet.sdk.vo.CompactBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DbIntegrationTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private var dao: CompactBlockDao? = null
    private var db: CompactBlockDb? = null

    @Before
    fun initDb() {
        // create real DB to inspect
        db = Room
            .databaseBuilder(ApplicationProvider.getApplicationContext(), CompactBlockDb::class.java, "compact-block.db")
            .fallbackToDestructiveMigration()
            .build()
            .apply { dao = complactBlockDao() }
    }

    @Test
    fun testDbExists() {
        assertNotNull(db)
    }

    @Test
    fun testDaoExists() {
        assertNotNull(dao)
        dao?.insert(CompactBlock(21))
        dao?.insert(CompactBlock(18))

        val block = dao?.findById(18)
        assertNotNull(block)
        assertEquals(18, block?.height)
    }
}