package cash.z.ecc.android.sdk.integration

import cash.z.ecc.android.sdk.annotation.MaintainedTest
import cash.z.ecc.android.sdk.annotation.TestPurpose
import cash.z.ecc.android.sdk.db.DatabaseCoordinator
import cash.z.ecc.android.sdk.ext.BlockExplorer
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.util.TestWallet
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.Response
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.DefaultAsserter.assertEquals
import kotlin.test.DefaultAsserter.assertTrue

// TODO [#650]: https://github.com/zcash/zcash-android-wallet-sdk/issues/650
// TODO [#650]: Move integration tests to separate module

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
    private val birthday: BlockHeight

) {

    val networkName = wallet.networkName
    val name = "$networkName wallet"

    @Test
    fun testFilePaths() {
        assertTrue(
            "$name has invalid DataDB file",
            wallet.initializer.rustBackend.dataDbFile.absolutePath.endsWith(
                "no_backup/co.electricoin.zcash/TestWallet_${networkName}_${DatabaseCoordinator.DB_DATA_NAME}"
            )
        )
        assertTrue(
            "$name has invalid CacheDB file",
            wallet.initializer.rustBackend.cacheDbFile.absolutePath.endsWith(
                "no_backup/co.electricoin.zcash/TestWallet_${networkName}_${DatabaseCoordinator.DB_CACHE_NAME}"
            )
        )
        assertTrue(
            "$name has invalid CacheDB params dir",
            wallet.initializer.rustBackend.saplingParamDir.endsWith(
                "no_backup/co.electricoin.zcash"
            )
        )
    }

    @Test
    fun testBirthday() {
        assertEquals(
            "$name has invalid birthday height",
            birthday,
            wallet.initializer.checkpoint.height
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

    @Ignore(
        "This test needs to be refactored to a separate test module. It causes SSLHandshakeException: Chain " +
            "validation failed on CI"
    )
    @Test
    fun testLatestHeight() = runBlocking {
        if (wallet.networkName == "mainnet") {
            val expectedHeight = BlockExplorer.fetchLatestHeight()

            // Fetch height directly because the synchronizer hasn't started, yet. Then we test the
            // result, only if there is no server communication problem.
            val downloaderHeight = runCatching {
                return@runCatching wallet.service.getLatestBlockHeight()
            }.onFailure {
                twig(it)
            }.getOrElse { return@runBlocking }

            assertTrue(downloaderHeight is Response.Success<BlockHeightUnsafe>)

            assertTrue(
                "${wallet.endpoint} ${wallet.networkName} Lightwalletd is too far behind. Downloader height $downloaderHeight is more than 10 blocks behind block explorer height $expectedHeight",
                expectedHeight - 10 < (downloaderHeight as Response.Success<BlockHeightUnsafe>).result.value
            )
        }
    }

    @Ignore(
        "This test needs to be refactored to a separate test module. It causes SSLHandshakeException: Chain " +
            "validation failed on CI"
    )
    @Test
    fun testSingleBlockDownload() = runBlocking {
        // Fetch height directly because the synchronizer hasn't started, yet. Then we test the
        // result, only if there is no server communication problem.
        val height = BlockHeight.new(wallet.network, 1_000_000)
        val block = runCatching {
            return@runCatching wallet.service.getBlockRange(
                BlockHeightUnsafe(height.value)..BlockHeightUnsafe(
                    height.value
                )
            ).first()
        }.onFailure {
            twig(it)
        }.getOrElse { return@runBlocking }

        assertTrue(
            "$networkName failed to return a proper block. Height was $block but we expected $height",
            block.height == height.value
        )
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
                BlockHeight.new(ZcashNetwork.Testnet, 1330000)
            ),
            // Mainnet wallet
            arrayOf(
                TestWallet(TestWallet.Backups.SAMPLE_WALLET, ZcashNetwork.Mainnet),
                "zxviews1q0hxkupsqqqqpqzsffgrk2smjuccedua7zswf5e3rgtv3ga9nhvhjug670egshd6me53r5n083s2m9mf4va4z7t39ltd3wr7hawnjcw09eu85q0ammsg0tsgx24p4ma0uvr4p8ltx5laum2slh2whc23ctwlnxme9w4dw92kalwk5u4wyem8dynknvvqvs68ktvm8qh7nx9zg22xfc77acv8hk3qqll9k3x4v2fa26puu2939ea7hy4hh60ywma69xtqhcy4037ne8g2sg8sq",
                "031c6355641237643317e2d338f5e8734c57e8aa8ce960ee22283cf2d76bef73be",
                BlockHeight.new(ZcashNetwork.Mainnet, 1000000)
            )
        )
    }
}
