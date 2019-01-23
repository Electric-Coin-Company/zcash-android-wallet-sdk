package cash.z.wallet.sdk.data

import android.content.Context
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import cash.z.wallet.sdk.dao.BlockDao
import cash.z.wallet.sdk.dao.NoteDao
import cash.z.wallet.sdk.dao.TransactionDao
import cash.z.wallet.sdk.jni.JniConverter
import cash.z.wallet.sdk.vo.Block
import cash.z.wallet.sdk.vo.Note
import cash.z.wallet.sdk.vo.Transaction
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import kotlin.random.Random

internal class PollingTransactionRepositoryTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: TransactionRepository
    private lateinit var noteDao: NoteDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var blockDao: BlockDao
    private val twig = TestLogTwig()
    private var ids = 0L
    private var heights: Int = 123_456
    private lateinit var balanceProvider: Iterator<Long>
    private val pollFrequency = 100L

    private lateinit var converter: JniConverter

    @Before
    fun setUp() {
        val dbName = "polling-test.db"
        val context = ApplicationProvider.getApplicationContext<Context>()
        converter = mock {
            on { getBalance(any()) }.thenAnswer { balanceProvider.next() }
        }
        repository = PollingTransactionRepository(context, dbName, pollFrequency, converter, twig) { db ->
            blockDao = db.blockDao()
            transactionDao = db.transactionDao()
            noteDao = db.noteDao()
        }
    }

    @After
    fun tearDown() {
        repository.stop()
        blockDao.deleteAll()

        // just verify the cascading deletes are working, for sanity
        assertEquals(0, blockDao.count())
        assertEquals(0, transactionDao.count())
        assertEquals(0, noteDao.count())
    }

    @Test
    fun testBalancesAreDistinct() = runBlocking<Unit> {
        val balanceList = listOf(1L, 1L, 2L, 2L, 3L, 3L, 4L, 4L)
        val iterations = balanceList.size
        balanceProvider = balanceList.iterator()

        insert(6) {
            repository.stop()
        }

        var distinctBalances = 0
        val balances = repository.balance()
        twig.twigTask("waiting for balance changes") {
            for (balance in balances) {
                twig.twig("found balance of $balance")
                distinctBalances++
            }
        }

        assertEquals(iterations, blockDao.count())
        assertEquals(balanceList.distinct().size, distinctBalances)

        // we at least requested the balance more times from the rust library than we got it in the channel
        // (meaning the duplicates were ignored)
        verify(converter, atLeast(distinctBalances + 1)).getBalance(anyString())
    }

    @Test
    fun testTransactionsAreNotLost() = runBlocking<Unit> {
        val iterations = 10
        balanceProvider = List(iterations + 1) { it.toLong() }.iterator()
        val transactionChannel = repository.transactions()
        repository.start(this)
        insert(iterations) {
            repeat(iterations) {
                assertNotNull("unexpected null for transaction number $it", transactionChannel.poll())
            }
            assertNull("transactions shouldn't remain", transactionChannel.poll())
            assertEquals("incorrect number of items in DB",  iterations, blockDao.count())
            repository.stop()
        }
    }

    /**
     * insert [count] items, then run the code block.
     */
    private fun CoroutineScope.insert(count: Int, block: suspend () -> Unit = {}) {
        repeat(count) {
            launch { insertItemDelayed(it * pollFrequency) }
        }
        launch { delay(pollFrequency * count * 2); block() }
    }

    private suspend fun insertItemDelayed(duration: Long) {
        twig.twig("delaying $duration")
        delay(duration)

        val block = createBlock()
        val transaction = createTransaction(block.height)
        val note = createNote(transaction.id)
        twig.twig("inserting note with value ${note.value}")
        blockDao.insert(block)
        transactionDao.insert(transaction)
        noteDao.insert(note)
    }


    private fun createBlock(): Block {
        return Block(heights++, System.currentTimeMillis().toInt(), byteArrayOf(heights.toByte()))
    }

    private fun createTransaction(blockId: Int): Transaction {
        return Transaction(ids++, byteArrayOf(ids.toByte()), blockId, null)
    }

    private fun createNote(id: Long): Note {
        return Note(
            id.toInt(),
            id.toInt(),
            value = Random.nextInt(0, 10)
        )
    }
}

class TestLogTwig : TroubleshootingTwig(printer = { msg: String -> Log.e("TEST_LOG", msg) })