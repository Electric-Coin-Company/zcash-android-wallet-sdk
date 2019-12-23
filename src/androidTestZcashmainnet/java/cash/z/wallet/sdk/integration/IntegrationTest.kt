package cash.z.wallet.sdk.integration

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.wallet.sdk.Initializer
import cash.z.wallet.sdk.Synchronizer
import cash.z.wallet.sdk.Synchronizer.Status.SYNCED
import cash.z.wallet.sdk.entity.isSubmitSuccess
import cash.z.wallet.sdk.ext.*
import cash.z.wallet.sdk.jni.RustBackend
import cash.z.wallet.sdk.service.LightWalletGrpcService
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
        val (height, hash, time, tree) = Initializer.loadBirthdayFromAssets(context, ZcashSdk.SAPLING_ACTIVATION_HEIGHT + 1)
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
                availableBalance = it.available
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
        synchronizer.balances.filter { it.available > 0 }.onEach {
            success = sendFunds()
        }.first()
        log("asserting $success")
        assertTrue(success)
    }

    private suspend fun sendFunds(): Boolean {
        val spendingKey = RustBackend().deriveSpendingKeys(seed)[0]
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

        const val host = "lightd-main.zecwallet.co"
        const val port = 443
        val seed = "cash.z.wallet.sdk.integration.IntegrationTest.seed.value.64bytes".toByteArray()
        val address = "zs1m30y59wxut4zk9w24d6ujrdnfnl42hpy0ugvhgyhr8s0guszutqhdj05c7j472dndjstulph74m"
        val toAddress = "zs1vp7kvlqr4n9gpehztr76lcn6skkss9p8keqs3nv8avkdtjrcctrvmk9a7u494kluv756jeee5k0"

        private val context = InstrumentationRegistry.getInstrumentation().context
        private val synchronizer: Synchronizer = Synchronizer(
            context,
            host,
            443,
            seed
        )

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