package cash.z.ecc.android.sdk.integration.darkside

// import cash.z.ecc.android.sdk.SdkSynchronizer
// import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
// import cash.z.ecc.android.sdk.ext.ScopedTest
// import cash.z.ecc.android.sdk.ext.twig
// import cash.z.ecc.android.sdk.util.DarksideTestCoordinator
// import kotlinx.coroutines.Job
// import kotlinx.coroutines.delay
// import kotlinx.coroutines.flow.launchIn
// import kotlinx.coroutines.flow.onEach
// import kotlinx.coroutines.runBlocking
// import org.junit.Assert.assertEquals
// import org.junit.BeforeClass
// import org.junit.Test

// class MultiAccountTest : ScopedTest() {
//
//    @Test
//    fun testTargetBlock_sanityCheck() {
//        with(sithLord) {
//            validator.validateMinHeightScanned(663250)
//            validator.validateMinBalance(200000)
//        }
//    }
//
//    @Test
//    fun testTargetBlock_send() = runBlocking {
//        with(sithLord) {
//
//            twig("<importing viewing key><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><>")
//            synchronizer.importViewingKey(secondKey)
//            twig("<DONE importing viewing key><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><>")
//
//            twig("IM GONNA SEND!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
//            sithLord.sendAndWait(testScope, spendingKey, 10000, secondAddress, "multi-account works!")
//            chainMaker.applySentTransactions()
//            await(targetHeight = 663251)
//
//            twig("done waiting for 663251!")
//            validator.validateMinHeightScanned(663251)
//
//            // balance before confirmations
//            validator.validateBalance(310000)
//
//            // add remaining confirmations
//            chainMaker.advanceBy(9)
//            await(targetHeight = 663260)
//
//            // balance after confirmations
//            validator.validateBalance(390000)
//
//            // check the extra viewing key balance!!!
//            val account1Balance = (synchronizer as SdkSynchronizer).processor.getBalanceInfo(1)
//            assertEquals(10000, account1Balance.totalZatoshi)
//            twig("done waiting for 663261!")
//        }
//    }
//
//
//    companion object {
//        private const val blocksUrl = "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/basic-reorg/before-reorg.txt"
//        private val sithLord = DarksideTestCoordinator("192.168.1.134")
//        private val secondAddress = "zs15tzaulx5weua5c7l47l4pku2pw9fzwvvnsp4y80jdpul0y3nwn5zp7tmkcclqaca3mdjqjkl7hx"
//        private val secondKey = "zxviews1q0w208wwqqqqpqyxp978kt2qgq5gcyx4er907zhczxpepnnhqn0a47ztefjnk65w2573v7g5fd3hhskrg7srpxazfvrj4n2gm4tphvr74a9xnenpaxy645dmuqkevkjtkf5jld2f7saqs3xyunwquhksjpqwl4zx8zj73m8gk2d5d30pck67v5hua8u3chwtxyetmzjya8jdjtyn2aum7au0agftfh5q9m4g596tev9k365s84jq8n3laa5f4palt330dq0yede053sdyfv6l"
//
//        @BeforeClass
//        @JvmStatic
//        fun startAllTests() {
//            sithLord.enterTheDarkside()
//            sithLord.chainMaker.simpleChain()
//            sithLord.startSync(classScope).await()
//        }
//    }
// }
