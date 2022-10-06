package cash.z.ecc.android.sdk.internal.transaction

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.annotation.MaintainedTest
import cash.z.ecc.android.sdk.annotation.TestPurpose
import cash.z.ecc.android.sdk.db.entity.EncodedTransaction
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.db.entity.isCancelled
import cash.z.ecc.android.sdk.internal.TroubleshootingTwig
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.service.LightWalletService
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.test.ScopedTest
import cash.z.ecc.fixture.DatabaseNameFixture
import cash.z.ecc.fixture.DatabasePathFixture
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.stub
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@MaintainedTest(TestPurpose.REGRESSION)
@RunWith(AndroidJUnit4::class)
@SmallTest
class PersistentTransactionManagerTest : ScopedTest() {

    @Mock lateinit var mockEncoder: TransactionEncoder

    @Mock lateinit var mockService: LightWalletService

    private val pendingDbFile = File(
        DatabasePathFixture.new(),
        DatabaseNameFixture.newDb(name = "PersistentTxMgrTest_Pending.db")
    ).apply {
        assertTrue(parentFile != null)
        parentFile!!.mkdirs()
        assertTrue(parentFile!!.exists())
        createNewFile()
        assertTrue(exists())
    }
    private lateinit var manager: OutboundTransactionManager

    @Before
    fun setup() {
        initMocks()
        deleteDb()
        manager = PersistentTransactionManager(context, mockEncoder, mockService, pendingDbFile)
    }

    private fun deleteDb() {
        pendingDbFile.deleteRecursively()
    }

    private fun initMocks() {
        MockitoAnnotations.openMocks(this)
        mockEncoder.stub {
            onBlocking {
                createTransaction(any(), any(), any(), any())
            }.thenAnswer {
                runBlocking {
                    delay(200)
                    EncodedTransaction(byteArrayOf(1, 2, 3), byteArrayOf(8, 9), 5_000_000)
                }
            }
        }
    }

    @Test
    fun testCancel() = runBlocking {
        var tx = manager.initSpend(Zatoshi(1234), "a", "b", Account.DEFAULT)
        assertFalse(tx.isCancelled())
        manager.cancel(tx.id)
        tx = manager.findById(tx.id)!!
        assertTrue(tx.isCancelled(), "Transaction was not cancelled")
    }

    @Test
    fun testAbort() = runBlocking {
        var tx: PendingTransaction? = manager.initSpend(Zatoshi(1234), "a", "b", Account.DEFAULT)
        assertNotNull(tx)
        manager.abort(tx)
        tx = manager.findById(tx.id)
        assertNull(tx, "Transaction was not removed from the DB")
    }

    companion object {
        @BeforeClass
        fun init() {
            Twig.plant(TroubleshootingTwig())
        }
    }
}
