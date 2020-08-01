package cash.z.ecc.android.sdk.transaction

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.db.entity.EncodedTransaction
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.db.entity.isCancelled
import cash.z.ecc.android.sdk.ext.ScopedTest
import cash.z.ecc.android.sdk.ext.TroubleshootingTwig
import cash.z.ecc.android.sdk.ext.Twig
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.service.LightWalletService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.stub
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class PersistentTransactionManagerTest : ScopedTest() {

    @Mock lateinit var mockEncoder: TransactionEncoder
    @Mock lateinit var mockService: LightWalletService

    val pendingDbName = "PersistentTxMgrTest_Pending.db"
    val dataDbName = "PersistentTxMgrTest_Data.db"
    private val context = InstrumentationRegistry.getInstrumentation().context
    private lateinit var manager: OutboundTransactionManager

    @Before
    fun setup() {
        initMocks()
        deleteDb()
        manager = PersistentTransactionManager(context, mockEncoder, mockService, pendingDbName)
    }

    private fun deleteDb() {
        context.getDatabasePath(pendingDbName).delete()
    }

    private fun initMocks() {
        MockitoAnnotations.initMocks(this)
        mockEncoder.stub {
            onBlocking {
                createTransaction(any(), any(), any(), any(), any())
            }.thenAnswer { invocation ->
                runBlocking {
                    delay(200)
                    EncodedTransaction(byteArrayOf(1,2,3), byteArrayOf(8,9), 5_000_000)
                }
            }
        }
    }

    @Test
    fun testCancellation_RaceCondition() = runBlocking {
        val tx = manager.initSpend(1234, "taddr", "memo-good", 0)
        val txFlow = manager.monitorById(tx.id)

        // encode TX
        testScope.launch {
            twig("ENCODE: start"); manager.encode("fookey", tx); twig("ENCODE: end")
        }

        // then cancel it before it is done encoding
        testScope.launch {
            delay(100)
            twig("CANCEL: start"); manager.cancel(tx.id); twig("CANCEL: end")
        }

        txFlow.drop(2).onEach {
            twig("found tx: $it")
            assertTrue("Expected the encoded tx to be cancelled but it wasn't", it.isCancelled())
            twig("found it to be successfully cancelled")
            testScope.cancel()
        }.launchIn(testScope).join()
    }

    @Test
    fun testCancel() = runBlocking {
        var tx = manager.initSpend(1234,  "a", "b", 0)
        assertFalse(tx.isCancelled())
        manager.cancel(tx.id)
        tx = manager.findById(tx.id)!!
        assertTrue("Transaction was not cancelled", tx.isCancelled())
    }

    @Test
    fun testAbort() = runBlocking {
        var tx: PendingTransaction? = manager.initSpend(1234,  "a", "b", 0)
        assertNotNull(tx)
        manager.abort(tx!!)
        tx = manager.findById(tx.id)
        assertNull("Transaction was not removed from the DB", tx)
    }

    companion object {
        @BeforeClass
        fun init() {
            Twig.plant(TroubleshootingTwig())
        }
    }
}