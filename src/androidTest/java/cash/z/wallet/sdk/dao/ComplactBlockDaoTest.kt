package cash.z.wallet.sdk.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.wallet.sdk.db.CompactBlockDb
import cash.z.wallet.sdk.entity.CompactBlock
import org.junit.*
import org.junit.Assert.*

class ComplactBlockDaoTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var dao: CompactBlockDao
    private lateinit var db: CompactBlockDb

    @Before
    fun initDb() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().context,
            CompactBlockDb::class.java
        )
            .build()
            .apply { dao = complactBlockDao() }
    }

    @After
    fun close() {
        db.close()
    }

    @Test
    fun testDbExists() {
        assertNotNull(db)
    }

    @Test
    fun testDaoExists() {
        assertNotNull(dao)
    }

    @Test
    fun testDaoInsert() {
        CompactBlock(343899, "sample".toByteArray()).let { block ->
            dao.insert(block)
            val result = dao.findById(block.height)
            assertEquals(block.height, result?.height)
            assertTrue(block.data.contentEquals(result!!.data))
            dao.delete(block)
        }
    }

}