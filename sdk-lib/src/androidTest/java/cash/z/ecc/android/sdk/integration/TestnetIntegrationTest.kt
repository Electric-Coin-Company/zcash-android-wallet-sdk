package cash.z.wallet.sdk.integration

import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.Synchronizer.Status.SYNCED
import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
import cash.z.ecc.android.sdk.ext.TroubleshootingTwig
import cash.z.ecc.android.sdk.ext.Twig
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.onFirst
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.test.ScopedTest
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool
import cash.z.ecc.android.sdk.type.NetworkType
import cash.z.ecc.android.sdk.type.ZcashNetwork
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
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
    fun testLatestBlockTest() {
        val service = LightWalletGrpcService(
            context,
            host,
        )
        val height = service.getLatestBlockHeight()
        assertTrue(height > saplingActivation)
    }

    @Test
    fun testLoadBirthday() {
        val (height, hash, time, tree) = WalletBirthdayTool.loadNearest(context, synchronizer.network, saplingActivation + 1)
        assertEquals(saplingActivation, height)
    }

    @Test
    @Ignore("This test is broken")
    fun getAddress() = runBlocking {
        assertEquals(address, synchronizer.getAddress())
    }

    // This is an extremely slow test; it is disabled so that we can get CI set up
    @Test
    @LargeTest
    @Ignore("This test is extremely slow")
    fun testBalance() = runBlocking {
        var availableBalance: Long = 0L
        synchronizer.saplingBalances.onFirst {
            availableBalance = it.availableZatoshi
        }

        synchronizer.status.filter { it == SYNCED }.onFirst {
            delay(100)
        }

        assertTrue(
            "No funds available when we expected a balance greater than zero!",
            availableBalance > 0
        )
    }

    @Test
    @Ignore("This test is broken")
    fun testSpend() = runBlocking {
        var success = false
        synchronizer.saplingBalances.filter { it.availableZatoshi > 0 }.onEach {
            success = sendFunds()
        }.first()
        log("asserting $success")
        assertTrue(success)
    }

    private suspend fun sendFunds(): Boolean {
        val spendingKey = DerivationTool.deriveSpendingKeys(seed, synchronizer.network)[0]
        log("sending to address")
        synchronizer.sendToAddress(
            spendingKey,
            ZcashSdk.MINERS_FEE_ZATOSHI,
            toAddress,
            "first mainnet tx from the SDK"
        ).filter { it?.isSubmitSuccess() == true }.onFirst {
            log("DONE SENDING!!!")
        }
        log("returning true from sendFunds")
        return true
    }

    fun log(message: String) {
        twig("\n---\n[TESTLOG]: $message\n---\n")
    }

    companion object {
        init { Twig.plant(TroubleshootingTwig()) }

        const val host = "lightwalletd.testnet.z.cash"
        private const val birthdayHeight = 963150
        private const val targetHeight = 663250
        private const val seedPhrase = "still champion voice habit trend flight survey between bitter process artefact blind carbon truly provide dizzy crush flush breeze blouse charge solid fish spread"
        val seed = "cash.z.ecc.android.sdk.integration.IntegrationTest.seed.value.64bytes".toByteArray()
        val address = "zs1m30y59wxut4zk9w24d6ujrdnfnl42hpy0ugvhgyhr8s0guszutqhdj05c7j472dndjstulph74m"
        val toAddress = "zs1vp7kvlqr4n9gpehztr76lcn6skkss9p8keqs3nv8avkdtjrcctrvmk9a7u494kluv756jeee5k0"

        private val context = InstrumentationRegistry.getInstrumentation().context
        private val initializer = Initializer(context) { config ->
            config.setNetwork(NetworkType.Testnet, host)
            config.importWallet(seed, birthdayHeight, NetworkType.Testnet)
        }
        private lateinit var synchronizer: Synchronizer

        @JvmStatic
        @BeforeClass
        fun startUp() {
            synchronizer = Synchronizer(initializer)
            synchronizer.start(classScope)
        }
    }
}
