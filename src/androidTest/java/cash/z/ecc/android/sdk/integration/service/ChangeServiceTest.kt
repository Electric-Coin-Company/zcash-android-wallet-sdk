package cash.z.ecc.android.sdk.integration.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import cash.z.ecc.android.sdk.block.CompactBlockDownloader
import cash.z.ecc.android.sdk.block.CompactBlockStore
import cash.z.ecc.android.sdk.exception.LightWalletException
import cash.z.ecc.android.sdk.exception.LightWalletException.ChangeServerException.*
import cash.z.ecc.android.sdk.ext.ScopedTest
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.service.LightWalletService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Spy

@RunWith(AndroidJUnit4::class)
class ChangeServiceTest : ScopedTest() {

    @Mock
    lateinit var mockBlockStore: CompactBlockStore
    var mockCloseable: AutoCloseable? = null

    @Spy
    val service = LightWalletGrpcService(context, ZcashSdk.DEFAULT_LIGHTWALLETD_HOST)

    lateinit var downloader: CompactBlockDownloader
    lateinit var otherService: LightWalletService

    @Before
    fun setup() {
        initMocks()
        downloader = CompactBlockDownloader(service, mockBlockStore)
        otherService = LightWalletGrpcService(context, "lightwalletd.electriccoin.co", 9067)
    }

    @After
    fun tearDown() {
        mockCloseable?.close()
    }

    private fun initMocks() {
        mockCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun testSanityCheck() {
        val result = service.getLatestBlockHeight()
        assertTrue(result > ZcashSdk.SAPLING_ACTIVATION_HEIGHT)
    }

    @Test
    fun testCleanSwitch() = runBlocking {
        downloader.changeService(otherService)
        val result = downloader.downloadBlockRange(900_000..901_000)
        assertEquals(1_001, result)
    }

    @Test
    fun testSwitchWhileActive() = runBlocking {
        val start = 900_000
        val count = 5
        val vendors = mutableListOf<String>()
        var oldVendor = downloader.getServerInfo().vendor
        val job = testScope.launch {
            repeat(count) {
                vendors.add(downloader.getServerInfo().vendor)
                twig("downloading from ${vendors.last()}")
                downloader.downloadBlockRange(start..(start + 100 * it))
                delay(10L)
            }
        }
        delay(30)
        testScope.launch {
            downloader.changeService(otherService)
        }
        job.join()
        assertTrue(vendors.count { it == oldVendor } < vendors.size)
        assertEquals(count, vendors.size)
    }


    @Test
    fun testSwitchToInvalidServer() = runBlocking {
        var caughtException: Throwable? = null
        downloader.changeService(LightWalletGrpcService(context, "invalid.lightwalletd")) {
            caughtException = it
        }
        assertNotNull("Using an invalid host should generate an exception.", caughtException)
        assertTrue(
            "Exception was of the wrong type.",
            caughtException is StatusException
        )
    }

    @Test
    fun testSwitchToTestnetFails() = runBlocking {
        var caughtException: Throwable? = null
        downloader.changeService(LightWalletGrpcService(context, "lightwalletd.testnet.electriccoin.co", 9067)) {
            caughtException = it
        }
        assertNotNull("Using an invalid host should generate an exception.", caughtException)
        assertTrue(
            "Exception was of the wrong type.",
            caughtException is ChainInfoNotMatching
        )
        (caughtException as ChainInfoNotMatching).propertyNames.let { props ->
            arrayOf("consensusBranchId", "saplingActivationHeight", "chainName").forEach {
                assertTrue(
                    "$it should be a non-matching property but properties were [$props]", props.contains(it, true)
                )
            }
        }
    }

}
