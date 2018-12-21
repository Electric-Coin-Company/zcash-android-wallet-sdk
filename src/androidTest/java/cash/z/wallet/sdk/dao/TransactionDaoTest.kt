package cash.z.wallet.sdk.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.wallet.sdk.db.CompactBlockDb
import cash.z.wallet.sdk.db.DerivedDataDb
import cash.z.wallet.sdk.vo.CompactBlock
import cash.z.wallet.sdk.vo.Transaction
import org.junit.*
import org.junit.Assert.*

class TransactionDaoTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var dao: TransactionDao
    private lateinit var db: DerivedDataDb

    @Before
    fun initDb() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().context,
            DerivedDataDb::class.java
        )
            .build()
            .apply { dao = transactionDao() }
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
        Transaction(4, "sample".toByteArray(), 356418, null).let { transaction ->
            dao.insert(transaction)
            val result = dao.findById(transaction.id)
            assertEquals(transaction.id, result?.id)
            assertTrue(transaction.transactionId.contentEquals(result!!.transactionId))
            dao.delete(transaction)
        }
    }

}