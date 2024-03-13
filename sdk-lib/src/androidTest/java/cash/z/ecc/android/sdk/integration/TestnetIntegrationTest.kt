package cash.z.ecc.android.sdk.integration

import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.Synchronizer.Status.SYNCED
import cash.z.ecc.android.sdk.WalletInitMode
import cash.z.ecc.android.sdk.ext.onFirst
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.test.ScopedTest
import cash.z.ecc.android.sdk.tool.CheckpointTool
import cash.z.ecc.android.sdk.tool.DerivationTool
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.new
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.CountDownLatch

class TestnetIntegrationTest : ScopedTest() {
    var stopWatch = CountDownLatch(1)
    val saplingActivation = synchronizer.network.saplingActivationHeight

    @Test
    @Ignore("This test is broken")
    fun testLatestBlockTest() =
        runTest {
            val service =
                LightWalletClient.new(
                    context,
                    lightWalletEndpoint
                )
            val height = service.getLatestBlockHeight()
            assertTrue(height is Response.Success<BlockHeightUnsafe>)
            assertTrue((height as Response.Success<BlockHeightUnsafe>).result.value > saplingActivation.value)
        }

    @Test
    fun testLoadBirthday() {
        val (height) =
            runBlocking {
                CheckpointTool.loadNearest(
                    context,
                    synchronizer.network,
                    saplingActivation + 1
                )
            }
        assertEquals(saplingActivation, height)
    }

    @Test
    @Ignore("This test is broken")
    fun getAddress() =
        runBlocking {
            assertEquals(address, synchronizer.getUnifiedAddress(Account.DEFAULT))
        }

    // This is an extremely slow test; it is disabled so that we can get CI set up
    @Test
    @LargeTest
    @Ignore("This test is extremely slow")
    fun testBalance() =
        runBlocking {
            var availableBalance: Zatoshi? = null
            synchronizer.saplingBalances.onFirst {
                availableBalance = it?.available
            }

            synchronizer.status.filter { it == SYNCED }.onFirst {
                delay(100)
            }

            assertTrue(
                availableBalance!!.value > 0
            )
        }

    @Test
    @Ignore("This test is broken")
    fun testSpend() =
        runBlocking {
            var success = false
            synchronizer.saplingBalances.filterNotNull().onEach {
                success = sendFunds()
            }.first()
            log("asserting $success")
            assertTrue(success)
        }

    private suspend fun sendFunds(): Boolean {
        val spendingKey =
            DerivationTool.getInstance().deriveUnifiedSpendingKey(
                seed,
                synchronizer.network,
                Account.DEFAULT
            )
        log("sending to address")
        synchronizer.createProposedTransactions(
            synchronizer.proposeTransfer(
                spendingKey.account,
                toAddress,
                Zatoshi(10_000L),
                "first mainnet tx from the SDK"
            ),
            spendingKey
        )
        return true
    }

    fun log(message: String) {
        Twig.debug { "\n---\n[TESTLOG]: $message\n---\n" }
    }

    @Suppress("UnusedPrivateProperty")
    companion object {
        val lightWalletEndpoint = LightWalletEndpoint("lightwalletd.testnet.z.cash", 9087, true)
        private const val BIRTHDAY_HEIGHT = 963150L
        private const val TARGET_HEIGHT = 663250
        private const val SEED_PHRASE =
            "still champion voice habit trend flight survey between bitter process" +
                " artefact blind carbon truly provide dizzy crush flush breeze blouse charge solid fish spread"
        val seed = "cash.z.ecc.android.sdk.integration.IntegrationTest.seed.value.64bytes".toByteArray()
        val address = "zs1m30y59wxut4zk9w24d6ujrdnfnl42hpy0ugvhgyhr8s0guszutqhdj05c7j472dndjstulph74m"
        val toAddress = "zs1vp7kvlqr4n9gpehztr76lcn6skkss9p8keqs3nv8avkdtjrcctrvmk9a7u494kluv756jeee5k0"

        private val context = InstrumentationRegistry.getInstrumentation().context
        private lateinit var synchronizer: Synchronizer

        @JvmStatic
        @BeforeClass
        fun startUp() {
            synchronizer =
                Synchronizer.newBlocking(
                    context,
                    ZcashNetwork.Testnet,
                    lightWalletEndpoint =
                    lightWalletEndpoint,
                    seed = seed,
                    birthday = BlockHeight.new(ZcashNetwork.Testnet, BIRTHDAY_HEIGHT),
                    // Using existing wallet init mode as simplification for the test
                    walletInitMode = WalletInitMode.ExistingWallet
                )
        }
    }
}
