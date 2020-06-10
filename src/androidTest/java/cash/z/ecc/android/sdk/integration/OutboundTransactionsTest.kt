//package cash.z.ecc.android.sdk.integration
//
//import cash.z.ecc.android.sdk.ext.ScopedTest
//import cash.z.ecc.android.sdk.util.DarksideTestCoordinator
//import org.junit.Before
//import org.junit.BeforeClass
//import org.junit.Test
//
//class OutboundTransactionsTest : ScopedTest() {
//
//    @Before
//    fun beforeEachTest() {
//        testCoordinator.clearUnminedTransactions()
//    }
//
//    @Test
//    fun testSendIncrementsTransaction() {
//        validator.validateTransactionCount(initialTxCount)
//        testCoordinator.sendTransaction(txAmount).awaitSync()
//        validator.validatTransactionCount(initialTxCount + 1)
//    }
//
//    @Test
//    fun testSendReducesBalance() {
//        validator.validateBalance(initialBalance)
//        testCoordinator.sendTransaction(txAmount).awaitSync()
//        validator.validateBalanceLessThan(initialBalance)
//    }
//
//    @Test
//    fun testTransactionPending() {
//        testCoordinator.sendTransaction(txAmount).awaitSync()
//        validator.validateTransactionPending(testCoordinator.lastTransactionId)
//    }
//
//    @Test
//    fun testTransactionConfirmations_1() {
//        testCoordinator.sendTransaction(txAmount).generateNextBlock().awaitSync()
//        validator.validateConfirmations(testCoordinator.lastTransactionId, 1)
//        validator.validateBalanceLessThan(initialBalance - txAmount)
//    }
//
//    @Test
//    fun testTransactionConfirmations_9() {
//        testCoordinator.sendTransaction(txAmount).generateNextBlock().advanceBlocksBy(8).awaitSync()
//        validator.validateConfirmations(testCoordinator.lastTransactionId, 9)
//        validator.validateBalanceLessThan(initialBalance - txAmount)
//    }
//
//    @Test
//    fun testTransactionConfirmations_10() {
//        testCoordinator.sendTransaction(txAmount).generateNextBlock().advanceBlocksBy(9).awaitSync()
//        validator.validateConfirmations(testCoordinator.lastTransactionId, 10)
//        validator.validateBalance(initialBalance - txAmount)
//    }
//
//    @Test
//    fun testTransactionExpiration() {
//        validator.validateBalance(initialBalance)
//
//        // pending initially
//        testCoordinator.sendTransaction(txAmount).awaitSync()
//        val id = testCoordinator.lastTransactionId
//        validator.validateTransactionPending(id)
//
//        // still pending after 9 blocks
//        testCoordinator.advanceBlocksBy(9).awaitSync()
//        validator.validateTransactionPending(id)
//        validator.validateBalanceLessThan(initialBalance)
//
//        // expired after 10 blocks
//        testCoordinator.advanceBlocksBy(1).awaitSync()
//        validator.validateTransactionExpired(id)
//
//        validator.validateBalance(initialBalance)
//    }
//
//
//
//    companion object {
//        private const val blocksUrl = "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/basic-reorg/before-reorg.txt"
//        private const val initialBalance = 1.234
//        private const val txAmount = 1.1
//        private const val initialTxCount = 3
//        private val testCoordinator = DarksideTestCoordinator()
//        private val validator = testCoordinator.validator
//
//        @BeforeClass
//        @JvmStatic
//        fun startAllTests() {
//            testCoordinator
//                .enterTheDarkside()
//                .resetBlocks(blocksUrl)
//        }
//    }
//}
