//package cash.z.ecc.android.sdk.integration
//
//import cash.z.ecc.android.sdk.ext.ScopedTest
//import cash.z.ecc.android.sdk.util.DarksideTestCoordinator
//import org.junit.Assert.assertFalse
//import org.junit.Assert.assertTrue
//import org.junit.BeforeClass
//import org.junit.Test
//
//class ReorgBasicTest : ScopedTest() {
//
//    private var callbackTriggered = false
//
//    @Test
//    fun testReorgChangesBlockHash() {
//        testCoordinator.resetBlocks(blocksUrl)
//        validator.validateBlockHash(targetHeight, targetHash)
//        testCoordinator.updateBlocks(reorgUrl)
//        validator.validateBlockHash(targetHeight, reorgHash)
//    }
//
//    @Test
//    fun testReorgTriggersCallback() {
//        callbackTriggered = false
//        testCoordinator.resetBlocks(blocksUrl)
//        testCoordinator.synchronizer.registerReorgListener(reorgCallback)
//        assertFalse(callbackTriggered)
//
//        testCoordinator.updateBlocks(reorgUrl).awaitSync()
//        assertTrue(callbackTriggered)
//        testCoordinator.synchronizer.unregisterReorgListener()
//    }
//
//    fun reorgCallback() {
//        callbackTriggered = true
//    }
//
//    companion object {
//        private const val blocksUrl = "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/basic-reorg/before-reorg.txt"
//        private const val reorgUrl = "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/basic-reorg/after-small-reorg.txt"
//        private const val targetHeight = 663250
//        private const val targetHash = "tbd"
//        private const val reorgHash = "tbd"
//        private val testCoordinator = DarksideTestCoordinator()
//        private val validator = testCoordinator.validator
//
//        @BeforeClass
//        @JvmStatic
//        fun startAllTests() {
//            testCoordinator.enterTheDarkside()
//        }
//    }
//}
