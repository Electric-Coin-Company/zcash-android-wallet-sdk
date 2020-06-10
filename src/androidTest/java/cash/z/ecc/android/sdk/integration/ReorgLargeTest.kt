//package cash.z.ecc.android.sdk.integration
//
//import androidx.test.platform.app.InstrumentationRegistry
//import cash.z.ecc.android.sdk.Initializer
//import cash.z.ecc.android.sdk.SdkSynchronizer
//import cash.z.ecc.android.sdk.Synchronizer
//import cash.z.ecc.android.sdk.ext.ScopedTest
//import cash.z.ecc.android.sdk.ext.import
//import cash.z.ecc.android.sdk.ext.twig
//import cash.z.ecc.android.sdk.util.DarksideApi
//import io.grpc.StatusRuntimeException
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.filter
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.flow.onEach
//import kotlinx.coroutines.runBlocking
//import org.junit.Assert.*
//import org.junit.Before
//import org.junit.BeforeClass
//import org.junit.Test
//
//class ReorgHandlingTest : ScopedTest() {
//
//    @Before
//    fun setup() {
//        timeout(30_000L) {
//            synchronizer.awaitSync()
//        }
//    }
//
//    @Test
//    fun testBeforeReorg_minHeight() = timeout(30_000L) {
//        // validate that we are synced, at least to the birthday height
//        synchronizer.validateMinSyncHeight(birthdayHeight)
//    }
//
//    @Test
//    fun testBeforeReorg_maxHeight() = timeout(30_000L) {
//        // validate that we are not synced beyond the target height
//        synchronizer.validateMaxSyncHeight(targetHeight)
//    }
//
//    @Test
//    fun testBeforeReorg_latestBlockHash() = timeout(30_000L) {
//        val latestBlock = getBlock(targetHeight)
//        assertEquals("foo", latestBlock.header.toStringUtf8())
//    }
//
//    @Test
//    fun testAfterSmallReorg_callbackTriggered() = timeout(30_000L) {
//        hadReorg = false
//        triggerSmallReorg()
//        assertTrue(hadReorg)
//    }
//
//    @Test
//    fun testAfterSmallReorg_callbackTriggered() = timeout(30_000L) {
//        hadReorg = false
//        triggerSmallReorg()
//        assertTrue(hadReorg)
//    }
////    @Test
////    fun testSync_100Blocks()= timeout(10_000L) {
////        // validate that we are synced below the target height, at first
////        synchronizer.validateMaxSyncHeight(targetHeight - 1)
////        // then trigger and await more blocks
////        synchronizer.awaitHeight(targetHeight)
////        // validate that we are above the target height afterward
////        synchronizer.validateMinSyncHeight(targetHeight)
////    }
//
//    private fun Synchronizer.awaitSync() = runBlocking<Unit> {
//        twig("***  Waiting for sync ***")
//        status.onEach {
//            twig("got processor status $it")
//            assertTrue("Error: Cannot complete test because the server is disconnected.", it != Synchronizer.Status.DISCONNECTED)
//            delay(1000)
//        }.filter { it == Synchronizer.Status.SYNCED }.first()
//        twig("***  Done waiting for sync! ***")
//    }
//
//    private fun Synchronizer.awaitHeight(height: Int) = runBlocking<Unit> {
//        twig("***  Waiting for block $height ***")
////        processorInfo.first { it.lastScannedHeight >= height }
//        processorInfo.onEach {
//            twig("got processor info $it")
//            delay(1000)
//        }.first { it.lastScannedHeight >= height }
//        twig("***  Done waiting for block $height! ***")
//    }
//
//    private fun Synchronizer.validateMinSyncHeight(minHeight: Int) = runBlocking<Unit> {
//        val info = processorInfo.first()
//        val lastDownloadedHeight = info.lastDownloadedHeight
//        assertTrue("Expected to be synced beyond $minHeight but the last downloaded block was" +
//                " $lastDownloadedHeight details: $info", lastDownloadedHeight >= minHeight)
//    }
//
//    private fun Synchronizer.validateMaxSyncHeight(maxHeight: Int) = runBlocking<Unit> {
//        val lastDownloadedHeight = processorInfo.first().lastScannedHeight
//        assertTrue("Did not expect to be synced beyond $maxHeight but we are synced to" +
//                " $lastDownloadedHeight", lastDownloadedHeight <= maxHeight)
//    }
//
//    private fun getBlock(height: Int) =
//        lightwalletd.getBlockRange(height..height).first()
//
//    private val lightwalletd
//        get() = (synchronizer as SdkSynchronizer).processor.downloader.lightwalletService
//
//    companion object {
//        private const val host = "192.168.1.134"
//        private const val port = 9067
//        private const val birthdayHeight = 663150
//        private const val targetHeight = 663200
//        private const val seedPhrase = "still champion voice habit trend flight survey between bitter process artefact blind carbon truly provide dizzy crush flush breeze blouse charge solid fish spread"
//        private val context = InstrumentationRegistry.getInstrumentation().context
//        private val initializer = Initializer(context, host, port, "ReorgHandlingTests")
//        private lateinit var synchronizer: Synchronizer
//        private lateinit var sithLord: DarksideApi
//
//        @BeforeClass
//        @JvmStatic
//        fun startOnce() {
//
//            sithLord = DarksideApi(context, host, port)
//            enterTheDarkside()
//
//            // don't start until after we enter the darkside (otherwise the we find no blocks to begin with and sleep for an interval)
//            synchronizer.start(classScope)
//        }
//
//        private fun enterTheDarkside() = runBlocking<Unit> {
//            // verify that we are on the darkside
//            try {
//                twig("entering the darkside")
//                var info = synchronizer.getServerInfo()
//                assertTrue(
//                    "Error: not on the darkside",
//                    info.chainName.contains("darkside")
//                            or info.vendor.toLowerCase().contains("darkside", true)
//                )
//                twig("initiating the darkside")
//                sithLord.initiate(birthdayHeight + 10)
//                info = synchronizer.getServerInfo()
//                assertTrue(
//                    "Error: server not configured for the darkside. Expected initial height of" +
//                    " $birthdayHeight but found ${info.blockHeight}", birthdayHeight <= info.blockHeight)
//                twig("darkside initiation complete!")
//            } catch (error: StatusRuntimeException) {
//                fail("Error while fetching server status. Testing cannot begin due to:" +
//                        " ${error.message}. Verify that the server is running")
//            }
//        }
//    }
//    /*
//
//beginning to process new blocks (with lower bound: 663050)...
//downloading blocks in range 663202..663202
//found 1 missing blocks, downloading in 1 batches of 100...
//downloaded 663202..663202 (batch 1 of 1) [663202..663202] | 10ms
//validating blocks in range 663202..663202 in db: /data/user/0/cash.z.ecc.android.sdk.test/databases/ReorgTest22_Cache.db
//offset = min(100, 10 * (1)) = 10
//lowerBound = max(663201 - 10, 663050) = 663191
//handling chain error at 663201 by rewinding to block 663191
//Chain error detected at height: 663201. Rewinding to: 663191
//beginning to process new blocks (with lower bound: 663050)...
//downloading blocks in range 663192..663202
//found 11 missing blocks, downloading in 1 batches of 100...
//downloaded 663192..663202 (batch 1 of 1) [663192..663202] | 8ms
//validating blocks in range 663192..663202 in db: /data/user/0/cash.z.ecc.android.sdk.test/databases/ReorgTest22_Cache.db
//offset = min(100, 10 * (2)) = 20
//lowerBound = max(663191 - 20, 663050) = 663171
//handling chain error at 663191 by rewinding to block 663171
//Chain error detected at height: 663191. Rewinding to: 663171
//beginning to process new blocks (with lower bound: 663050)...
//downloading blocks in range 663172..663202
//found 31 missing blocks, downloading in 1 batches of 100...
//downloaded 663172..663202 (batch 1 of 1) [663172..663202] | 15ms
//validating blocks in range 663172..663202 in db: /data/user/0/cash.z.ecc.android.sdk.test/databases/ReorgTest22_Cache.db
//scanning blocks for range 663172..663202 in batches
//batch scanned: 663202/663202
//batch scan complete!
//Successfully processed new blocks. Sleeping for 20000ms
//
//     */
////
////    @Test
////    fun testHeightChange() {
////        setTargetHeight(targetHeight)
////        synchronizer.validateSyncedTo(targetHeight)
////    }
////
////    @Test
////    fun testSmallReorgSync() {
////        verifyReorgSync(smallReorgSize)
////    }
////
////    @Test
////    fun testSmallReorgCallback() {
////        verifyReorgCallback(smallReorgSize)
////    }
////
////    @Test
////    fun testLargeReorgSync() {
////        verifyReorgSync(largeReorgSize)
////    }
////
////    @Test
////    fun testLargeReorgCallback() {
////        verifyReorgCallback(largeReorgSize)
////    }
////
////
////    //
////    // Helper Functions
////    //
////
////    fun verifyReorgSync(reorgSize: Int) {
////        setTargetHeight(targetHeight)
////        synchronizer.validateSyncedTo(targetHeight)
////        getHash(targetHeight).let { initialHash ->
////            setReorgHeight(targetHeight - reorgSize)
////            synchronizer.validateSyncedTo(targetHeight)
////            assertNotEquals("Hash should change after a reorg", initialHash, getHash(targetHeight))
////        }
////    }
////
////    fun verifyReorgCallback(reorgSize: Int) {
////        setTargetHeight(targetHeight)
////        synchronizer.validateSyncedTo(targetHeight)
////        getHash(targetHeight).let { initialHash ->
////            setReorgHeight(targetHeight - 10)
////            synchronizer.validateReorgCallback()
////        }
////    }
//
//
//}
//
