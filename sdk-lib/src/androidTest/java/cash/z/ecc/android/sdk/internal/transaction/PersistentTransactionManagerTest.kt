package cash.z.ecc.android.sdk.internal.transaction

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.annotation.MaintainedTest
import cash.z.ecc.android.sdk.annotation.TestPurpose
import cash.z.ecc.android.sdk.internal.db.commonDatabaseBuilder
import cash.z.ecc.android.sdk.internal.db.pending.PendingTransactionDb
import cash.z.ecc.android.sdk.internal.model.EncodedTransaction
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.PendingTransaction
import cash.z.ecc.android.sdk.model.TransactionRecipient
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.test.ScopedTest
import cash.z.ecc.android.sdk.test.getAppContext
import cash.z.ecc.fixture.DatabaseNameFixture
import cash.z.ecc.fixture.DatabasePathFixture
import co.electriccoin.lightwallet.client.LightWalletClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.stub
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@MaintainedTest(TestPurpose.REGRESSION)
@RunWith(AndroidJUnit4::class)
@SmallTest
class PersistentTransactionManagerTest : ScopedTest() {

    @Mock
    internal lateinit var mockEncoder: TransactionEncoder

    @Mock
    lateinit var mockService: LightWalletClient

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
        val db = commonDatabaseBuilder(
            getAppContext(),
            PendingTransactionDb::class.java,
            pendingDbFile
        ).build()
        manager = PersistentTransactionManager(db, ZcashNetwork.Mainnet, mockEncoder, mockService)
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
                    EncodedTransaction(
                        FirstClassByteArray(byteArrayOf(1, 2, 3)),
                        FirstClassByteArray(
                            byteArrayOf(
                                8,
                                9
                            )
                        ),
                        BlockHeight.new(ZcashNetwork.Mainnet, 5_000_000)
                    )
                }
            }
        }
    }

    @Test
    fun testAbort() = runBlocking {
        var tx: PendingTransaction? = manager.initSpend(
            Zatoshi(1234),
            TransactionRecipient.Address("a"),
            "b",
            Account.DEFAULT
        )
        assertNotNull(tx)
        manager.abort(tx)
        tx = manager.findById(tx.id)
        assertNull(tx, "Transaction was not removed from the DB")
    }

    companion object {
    }
}
