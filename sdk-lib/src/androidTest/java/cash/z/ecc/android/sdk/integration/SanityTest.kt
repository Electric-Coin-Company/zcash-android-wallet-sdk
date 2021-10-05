package cash.z.ecc.android.sdk.integration

import cash.z.ecc.android.sdk.annotation.MaintainedTest
import cash.z.ecc.android.sdk.annotation.TestPurpose
import cash.z.ecc.android.sdk.ext.BlockExplorer
import cash.z.ecc.android.sdk.type.NetworkType
import cash.z.ecc.android.sdk.type.ZcashNetwork
import cash.z.ecc.android.sdk.util.TestWallet
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * This test is intended to run to make sure that basic things are functional and pinpoint what is
 * not working. It was originally developed after a major refactor to find what broke.
 */
@MaintainedTest(TestPurpose.COMMIT)
@RunWith(Parameterized::class)
class SanityTest(
    private val wallet: TestWallet,
    private val extfvk: String,
    private val extpub: String,
    private val birthday: Int,

) {

    val networkName = wallet.networkName
    val name = "$networkName wallet"

    @Test
    fun testNotPlaintext() {
        val message =
            "is using plaintext. This will cause problems for the test. Ensure that the `lightwalletd_allow_very_insecure_connections` resource value is false"
        assertFalse("$name $message", wallet.service.connectionInfo.usePlaintext)
    }

    @Test
    fun testFilePaths() {
        assertEquals(
            "$name has invalid DataDB file",
            "/data/user/0/cash.z.ecc.android.sdk.test/databases/TestWallet_${networkName}_Data.db",
            wallet.initializer.rustBackend.pathDataDb
        )
        assertEquals(
            "$name has invalid CacheDB file",
            "/data/user/0/cash.z.ecc.android.sdk.test/databases/TestWallet_${networkName}_Cache.db",
            wallet.initializer.rustBackend.pathCacheDb
        )
        assertEquals(
            "$name has invalid CacheDB params dir",
            "/data/user/0/cash.z.ecc.android.sdk.test/cache/params",
            wallet.initializer.rustBackend.pathParamsDir
        )
    }

    @Test
    fun testBirthday() {
        assertEquals(
            "$name has invalid birthday height",
            birthday,
            wallet.initializer.birthday.height
        )
    }

    @Test
    fun testViewingKeys() {
        assertEquals(
            "$name has invalid extfvk",
            extfvk,
            wallet.initializer.viewingKeys[0].extfvk
        )
        assertEquals(
            "$name has invalid extpub",
            extpub,
            wallet.initializer.viewingKeys[0].extpub
        )
    }

    @Test
    fun testServerConnection() {
        assertEquals(
            "$name has an invalid server connection",
            "$networkName.lightwalletd.com:9067?usePlaintext=false",
            wallet.connectionInfo
        )
    }

    @Test
    fun testLatestHeight() = runBlocking {
        if (wallet.networkName == "mainnet") {
            val expectedHeight = BlockExplorer.fetchLatestHeight()
            // fetch height directly because the synchronizer hasn't started, yet
            val downloaderHeight = wallet.service.getLatestBlockHeight()
            val info = wallet.connectionInfo
            assertTrue(
                "$info\n ${wallet.networkName} Lightwalletd is too far behind. Downloader height $downloaderHeight is more than 10 blocks behind block explorer height $expectedHeight",
                expectedHeight - 10 < downloaderHeight
            )
        }
    }

    @Test
    fun testSingleBlockDownload() = runBlocking {
        // fetch block directly because the synchronizer hasn't started, yet
        val height = 1_000_000
        val block = wallet.service.getBlockRange(height..height)[0]
        assertTrue("$networkName failed to return a proper block. Height was ${block.height} but we expected $height", block.height.toInt() == height)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun wallets() = listOf(
            // Testnet wallet
            arrayOf(
                TestWallet(TestWallet.Backups.SAMPLE_WALLET),
                "zxviewtestsapling1qv0ue89kqqqqpqqyt4cl5wvssx4wqq30e5m948p07dnwl9x3u75vvnzvjwwpjkrf8yk2gva0kkxk9p8suj4xawlzw9pajuxgap83wykvsuyzfrm33a2p2m4jz2205kgzx0l2lj2kyegtnuph6crkyvyjqmfxut84nu00wxgrstu5fy3eu49nzl8jzr4chmql4ysgg2t8htn9dtvxy8c7wx9rvcerqsjqm6lqln9syk3g8rr3xpy3l4nj0kawenzpcdtnv9qmy98vdhqzaf063",
                "0234965f30c8611253d035f44e68d4e2ce82150e8665c95f41ccbaf916b16c69d8",
                1320000
            ),
            // Mainnet wallet
            arrayOf(
                TestWallet(TestWallet.Backups.SAMPLE_WALLET, NetworkType.Mainnet),
                "zxviews1q0hxkupsqqqqpqzsffgrk2smjuccedua7zswf5e3rgtv3ga9nhvhjug670egshd6me53r5n083s2m9mf4va4z7t39ltd3wr7hawnjcw09eu85q0ammsg0tsgx24p4ma0uvr4p8ltx5laum2slh2whc23ctwlnxme9w4dw92kalwk5u4wyem8dynknvvqvs68ktvm8qh7nx9zg22xfc77acv8hk3qqll9k3x4v2fa26puu2939ea7hy4hh60ywma69xtqhcy4037ne8g2sg8sq",
                "031c6355641237643317e2d338f5e8734c57e8aa8ce960ee22283cf2d76bef73be",
                1000000
            )
        )
    }
}
