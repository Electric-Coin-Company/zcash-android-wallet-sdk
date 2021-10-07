package cash.z.ecc.android.sdk.darkside // package cash.z.ecc.android.sdk.integration
//
// import cash.z.ecc.android.sdk.test.ScopedTest
// import cash.z.ecc.android.sdk.internal.twigTask
// import cash.z.ecc.android.sdk.darkside.test.DarksideTestCoordinator
// import kotlinx.coroutines.runBlocking
// import org.junit.BeforeClass
// import org.junit.Test
//
// class MultiAccountIntegrationTest : ScopedTest() {
//
//    /**
//     * Test multiple viewing keys by doing the following:
//     *
//     * - sync "account A" with 100 test blocks containing:
//     *   (in zatoshi) four 100_000 notes and one 10_000 note
//     * - import a viewing key for "account B"
//     * - send a 10_000 zatoshi transaction from A to B
//     * - include that tx in the next block and mine that block (on the darkside), then scan it
//     * - verify that A's balance reflects a single 100_000 note being spent but pending confirmations
//     * - advance the chain by 9 more blocks to reach 10 confirmations
//     * - verify that the change from the spent note is reflected in A's balance
//     * - check B's balance and verify that it received the full 10_000 (i.e. that A paid the mining fee)
//     *
//     * Although we sent funds to an address, the synchronizer has both spending keys so it is able
//     * to track transactions for both addresses!
//     */
//    @Test
//    fun testViewingKeyImport() = runBlocking {
//        validatePreConditions()
//
//        with(sithLord) {
//            twigTask("importing viewing key") {
//                synchronizer.importViewingKey(secondKey)
//            }
//
//            twigTask("Sending funds") {
//                sithLord.createAndSubmitTx(10_000, secondAddress, "multi-account works!")
//                chainMaker.applyPendingTransactions(663251)
//                await(targetHeight = 663251)
//            }
//            // verify that the transaction block height was scanned
//            validator.validateMinHeightScanned(663251)
//
//            // balance before confirmations (the large 100_000 note gets selected)
//            validator.validateBalance(310_000)
//
//            // add remaining confirmations so that funds become spendable and await until they're scanned
//            chainMaker.advanceBy(9)
//            await(targetHeight = 663260)
//
//            // balance after confirmations
//            validator.validateBalance(390_000)
//
//            // check the extra viewing key balance!!!
//            // accountIndex 1 corresponds to the imported viewingKey for the address where we sent the funds!
//            validator.validateBalance(available = 10_000, accountIndex = 1)
//        }
//    }
//
//    /**
//     * Verify that before the integration test begins, the wallet is synced up to the expected block
//     * and contains the expected balance.
//     */
//    private fun validatePreConditions() {
//        with(sithLord) {
//            twigTask("validating preconditions") {
//                validator.validateMinHeightScanned(663250)
//                validator.validateMinBalance(410_000)
//            }
//        }
//    }
//
//
//    companion object {
//        private val sithLord = DarksideTestCoordinator()
//        private val secondAddress = "zs15tzaulx5weua5c7l47l4pku2pw9fzwvvnsp4y80jdpul0y3nwn5zp7tmkcclqaca3mdjqjkl7hx"
//        private val secondKey = "zxviews1q0w208wwqqqqpqyxp978kt2qgq5gcyx4er907zhczxpepnnhqn0a47ztefjnk65w2573v7g5fd3hhskrg7srpxazfvrj4n2gm4tphvr74a9xnenpaxy645dmuqkevkjtkf5jld2f7saqs3xyunwquhksjpqwl4zx8zj73m8gk2d5d30pck67v5hua8u3chwtxyetmzjya8jdjtyn2aum7au0agftfh5q9m4g596tev9k365s84jq8n3laa5f4palt330dq0yede053sdyfv6l"
//
//        @BeforeClass
//        @JvmStatic
//        fun startAllTests() {
//            sithLord.enterTheDarkside()
//            sithLord.chainMaker.makeSimpleChain()
//            sithLord.startSync(classScope).await()
//        }
//    }
// }
