package cash.z.ecc.android.sdk.integration.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.annotation.MaintainedTest
import cash.z.ecc.android.sdk.annotation.TestPurpose
import cash.z.ecc.android.sdk.exception.LightWalletException.ChangeServerException.ChainInfoNotMatching
import cash.z.ecc.android.sdk.exception.LightWalletException.ChangeServerException.StatusException
import cash.z.ecc.android.sdk.internal.block.CompactBlockDownloader
import cash.z.ecc.android.sdk.internal.block.CompactBlockStore
import cash.z.ecc.android.sdk.internal.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.internal.service.LightWalletService
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.LightWalletEndpoint
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.test.ScopedTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Spy

@MaintainedTest(TestPurpose.REGRESSION)
@RunWith(AndroidJUnit4::class)
@SmallTest
class ChangeServiceTest : ScopedTest() {

    val network = ZcashNetwork.Mainnet
    val lightWalletEndpoint = LightWalletEndpoint("mainnet.lightwalletd.com", 9087)

    @Mock
    lateinit var mockBlockStore: CompactBlockStore
    var mockCloseable: AutoCloseable? = null

    @Spy
    val service = LightWalletGrpcService(context, network, lightWalletEndpoint)

    lateinit var downloader: CompactBlockDownloader
    lateinit var otherService: LightWalletService

    @Before
    fun setup() {
        initMocks()
        downloader = CompactBlockDownloader(service, mockBlockStore)
        otherService = LightWalletGrpcService(context, LightWalletEndpoint("lightwalletd.electriccoin.co", 9087))
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
        assertTrue(result > network.saplingActivationHeight)
    }

    @Test
    fun testCleanSwitch() = runBlocking {
        downloader.changeService(otherService)
        val result = downloader.downloadBlockRange(BlockHeight.new(network, 900_000)..BlockHeight.new(network, 901_000))
        assertEquals(1_001, result)
    }

    /**
     * Repeatedly connect to servers and download a range of blocks. Switch part way through and
     * verify that the servers change over, even while actively downloading.
     */
    @Test
    @Ignore("This test is broken")
    fun testSwitchWhileActive() = runBlocking {
        val start = BlockHeight.new(ZcashNetwork.Mainnet, 900_000)
        val count = 5
        val differentiators = mutableListOf<String>()
        var initialValue = downloader.getServerInfo().buildUser
        val job = testScope.launch {
            repeat(count) {
                differentiators.add(downloader.getServerInfo().buildUser)
                twig("downloading from ${differentiators.last()}")
                downloader.downloadBlockRange(start..(start + 100 * it))
                delay(10L)
            }
        }
        delay(30)
        testScope.launch {
            downloader.changeService(otherService)
        }
        job.join()
        assertTrue(differentiators.count { it == initialValue } < differentiators.size)
        assertEquals(count, differentiators.size)
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
        downloader.changeService(LightWalletGrpcService(context, ZcashNetwork.Testnet)) {
            caughtException = it
        }
        assertNotNull("Using an invalid host should generate an exception.", caughtException)
        assertTrue(
            "Exception was of the wrong type. Expected ${ChainInfoNotMatching::class.simpleName} but was ${caughtException!!::class.simpleName}",
            caughtException is ChainInfoNotMatching
        )
        (caughtException as ChainInfoNotMatching).propertyNames.let { props ->
            arrayOf("saplingActivationHeight", "chainName").forEach {
                assertTrue(
                    "$it should be a non-matching property but properties were [$props]",
                    props.contains(it, true)
                )
            }
        }
    }
}
