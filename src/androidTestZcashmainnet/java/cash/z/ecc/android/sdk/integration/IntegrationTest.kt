package cash.z.ecc.android.sdk.integration

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.Synchronizer.Status.SYNCED
import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
import cash.z.ecc.android.sdk.ext.*
import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.CountDownLatch

class IntegrationTest {

    var stopWatch = CountDownLatch(1)

    @Test
    fun testLatestBlockTest() {
        val service = LightWalletGrpcService(
            context,
            host,
            port)
        val height = service.getLatestBlockHeight()
        assertTrue(height > ZcashSdk.SAPLING_ACTIVATION_HEIGHT)
    }

    @Test
    fun testLoadBirthday() {
        val (height, hash, time, tree) = WalletBirthdayTool.loadNearest(context, ZcashSdk.SAPLING_ACTIVATION_HEIGHT + 1)
        assertEquals(ZcashSdk.SAPLING_ACTIVATION_HEIGHT, height)
    }

    @Test
    fun getAddress() = runBlocking {
        assertEquals(address, synchronizer.getAddress())
    }

    @Test
    fun testBalance() = runBlocking {
        var availableBalance: Long = 0L
        synchronizer.balances.onFirst {
                availableBalance = it.availableZatoshi
        }

        synchronizer.status.filter { it == SYNCED }.onFirst {
            delay(100)
        }

        assertTrue("No funds available when we expected a balance greater than zero!",
            availableBalance > 0)
    }

    @Test
    @Ignore
    fun testSpend() = runBlocking {
        var success = false
        synchronizer.balances.filter { it.availableZatoshi > 0 }.onEach {
            success = sendFunds()
        }.first()
        log("asserting $success")
        assertTrue(success)
    }

    private suspend fun sendFunds(): Boolean {
        val spendingKey = DerivationTool.deriveSpendingKeys(seed)[0]
        log("sending to address")
        synchronizer.sendToAddress(
            spendingKey,
            ZcashSdk.MINERS_FEE_ZATOSHI,
            toAddress,
            "first mainnet tx from the SDK"
        ).filter { it.isSubmitSuccess() }.onFirst {
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

        const val host = "lightd-main.zecwallet.co"
        const val port = 443
        val seed = "cash.z.ecc.android.sdk.integration.IntegrationTest.seed.value.64bytes".toByteArray()
        val birthdayHeight = 843_000
        val address = "zs1m30y59wxut4zk9w24d6ujrdnfnl42hpy0ugvhgyhr8s0guszutqhdj05c7j472dndjstulph74m"
        val toAddress = "zs1vp7kvlqr4n9gpehztr76lcn6skkss9p8keqs3nv8avkdtjrcctrvmk9a7u494kluv756jeee5k0"

        private val context = InstrumentationRegistry.getInstrumentation().context
        private val initializer = Initializer(context) { config ->
            config.setSeed(seed)
            config.server(host, port)
            config.setBirthdayHeight(birthdayHeight)
        }
        private val synchronizer: Synchronizer = Synchronizer(initializer)

        @JvmStatic
        @BeforeClass
        fun startUp() {
            synchronizer.start()
        }

        @JvmStatic
        @AfterClass
        fun tearDown() {
            synchronizer.stop()
        }
    }
}
