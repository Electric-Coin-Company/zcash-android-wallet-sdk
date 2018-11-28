package cash.z.wallet.sdk.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.wallet.sdk.db.CompactBlockDb
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ComplactBlockDaoTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private var dao: CompactBlockDao? = null
    private var db: CompactBlockDb? = null

    @Before
    fun initDb() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().context,
            CompactBlockDb::class.java
        )
            .build()
            // .apply { dao = complactBlockDao() }
    }

    @Test
    fun testDbExists() {
        assertNotNull(db)
    }

    @Test
    fun testDaoExists() {
        assertNotNull(dao)
    }
}