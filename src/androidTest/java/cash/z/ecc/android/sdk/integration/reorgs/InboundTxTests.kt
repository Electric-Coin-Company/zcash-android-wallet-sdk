package cash.z.ecc.android.sdk.integration.reorgs

import cash.z.ecc.android.sdk.ext.ScopedTest
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.util.DarksideTestCoordinator
import org.junit.BeforeClass
import org.junit.Test

class InboundTxTests : ScopedTest() {

    @Test
    fun testTargetBlock_downloaded() {
        validator.validateMinHeightDownloaded(firstBlock)
    }

    @Test
    fun testTargetBlock_scanned() {
        validator.validateMinHeightScanned(targetTxBlock - 1)
    }

    @Test
    fun testLatestHeight() {
        validator.validateLatestHeight(targetTxBlock - 1)
    }

    @Test
    fun testTxCountInitial() {
        validator.validateTxCount(0)
    }

    @Test
    fun testTxCountAfter() {
        twig("ADDING TRANSACTIONS!!!")
        // add 2 transactions to block 663188 and 'mine' that block
        addTransactions(targetTxBlock, tx663174, tx663188)
        sithLord.await(timeout = 30_000L, targetHeight = targetTxBlock)
        validator.validateTxCount(2)
    }

    private fun addTransactions(targetHeight: Int, vararg txs: String) {
        val overwriteBlockCount = 5
        chainMaker
//            .stageEmptyBlocks(targetHeight, overwriteBlockCount)
            .stageTransactions(targetHeight, *txs)
            .applyTipHeight(targetHeight)
    }


    companion object {
        private const val blocksUrl = "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/basic-reorg/before-reorg.txt"
        private const val tx663174 = "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/0821a89be7f2fc1311792c3fa1dd2171a8cdfb2effd98590cbd5ebcdcfcf491f.txt"
        private const val tx663188 = "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/15a677b6770c5505fb47439361d3d3a7c21238ee1a6874fdedad18ae96850590.txt"
        private const val txIndexReorg = "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/tx-index-reorg/t1.txt"
        private val txSend = arrayOf(
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/t-shielded-spend.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/c9e35e6ff444b071d63bf9bab6480409d6361760445c8a28d24179adb35c2495.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/72a29d7db511025da969418880b749f7fc0fc910cdb06f52193b5fa5c0401d9d.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/ff6ea36765dc29793775c7aa71de19fca039c5b5b873a0497866e9c4bc48af01.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/34e507cab780546f980176f3ff2695cd404917508c7e5ee18cc1d2ff3858cb08.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/6edf869063eccff3345676b0fed9f1aa6988fb2524e3d9ca7420a13cfadcd76c.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/de97394ae220c28a33ba78b944e82dabec8cb404a4407650b134b3d5950358c0.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/4eaa902279f8380914baf5bcc470d8b7c11d84fda809f67f517a7cb48912b87b.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/73c5edf8ffba774d99155121ccf07e67fbcf14284458f7e732751fea60d3bcbc.txt"
        )

        private val txRecv = arrayOf(
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/8f064d23c66dc36e32445e5f3b50e0f32ac3ddb78cff21fb521eb6c19c07c99a.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/15a677b6770c5505fb47439361d3d3a7c21238ee1a6874fdedad18ae96850590.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/d2e7be14bbb308f9d4d68de424d622cbf774226d01cd63cc6f155fafd5cd212c.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/e6566be3a4f9a80035dab8e1d97e40832a639e3ea938fb7972ea2f8482ff51ce.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/0821a89be7f2fc1311792c3fa1dd2171a8cdfb2effd98590cbd5ebcdcfcf491f.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/e9527891b5d43d1ac72f2c0a3ac18a33dc5a0529aec04fa600616ed35f8123f8.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/4dcc95dd0a2f1f51bd64bb9f729b423c6de1690664a1b6614c75925e781662f7.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/75f2cdd2ff6a94535326abb5d9e663d53cbfa5f31ebb24b4d7e420e9440d41a2.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/7690c8ec740c1be3c50e2aedae8bf907ac81141ae8b6a134c1811706c73f49a6.txt",
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/71935e29127a7de0b96081f4c8a42a9c11584d83adedfaab414362a6f3d965cf.txt"
        )

        private const val firstBlock = 663150
        private const val targetTxBlock = 663188
        private const val lastBlockHash = "2fc7b4682f5ba6ba6f86e170b40f0aa9302e1d3becb2a6ee0db611ff87835e4a"
        private val sithLord = DarksideTestCoordinator("192.168.1.134")
        private val validator = sithLord.validator
        private val chainMaker = sithLord.chainMaker

        @BeforeClass
        @JvmStatic
        fun startAllTests() {
            sithLord.enterTheDarkside()

            chainMaker
                .resetBlocks(blocksUrl, startHeight = firstBlock, tipHeight = targetTxBlock)
                .stageEmptyBlocks(firstBlock + 1, 100)
                .applyTipHeight(targetTxBlock - 1)

            sithLord.startSync(classScope).await()
        }
    }
}
