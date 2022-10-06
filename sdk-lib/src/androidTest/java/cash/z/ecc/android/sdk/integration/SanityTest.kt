package cash.z.ecc.android.sdk.integration

import androidx.test.core.app.ApplicationProvider
import cash.z.ecc.android.sdk.DefaultSynchronizerFactory
import cash.z.ecc.android.sdk.annotation.MaintainedTest
import cash.z.ecc.android.sdk.annotation.TestPurpose
import cash.z.ecc.android.sdk.db.DatabaseCoordinator
import cash.z.ecc.android.sdk.ext.BlockExplorer
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.util.TestWallet
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.DefaultAsserter.assertTrue

// TODO [#650]: https://github.com/zcash/zcash-android-wallet-sdk/issues/650

/**
 * This test is intended to run to make sure that basic things are functional and pinpoint what is
 * not working. It was originally developed after a major refactor to find what broke.
 */
@MaintainedTest(TestPurpose.COMMIT)
@RunWith(Parameterized::class)
class SanityTest(
    private val wallet: TestWallet,
    private val encoding: String,
    private val birthday: BlockHeight

) {

    val networkName = wallet.networkName
    val name = "$networkName wallet"

    @Test
    fun testFilePaths() {
        val rustBackend = runBlocking {
            DefaultSynchronizerFactory.defaultRustBackend(
                ApplicationProvider.getApplicationContext(),
                ZcashNetwork.Testnet,
                "TestWallet",
                TestWallet.Backups.SAMPLE_WALLET.testnetBirthday
            )
        }

        assertTrue(
            "$name has invalid DataDB file",
            rustBackend.dataDbFile.absolutePath.endsWith(
                "no_backup/co.electricoin.zcash/TestWallet_${networkName}_${DatabaseCoordinator.DB_DATA_NAME}"
            )
        )
        assertTrue(
            "$name has invalid CacheDB file",
            rustBackend.cacheDbFile.absolutePath.endsWith(
                "no_backup/co.electricoin.zcash/TestWallet_${networkName}_${DatabaseCoordinator.DB_CACHE_NAME}"
            )
        )
        assertTrue(
            "$name has invalid CacheDB params dir",
            rustBackend.pathParamsDir.endsWith(
                "cache/params"
            )
        )
    }

    @Test
    @Ignore(
        "This test needs to be refactored to a separate test module. It causes SSLHandshakeException: Chain " +
            "validation failed on CI"
    )
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

            assertTrue(
                "${wallet.endpoint} ${wallet.networkName} Lightwalletd is too far behind. Downloader height $downloaderHeight is more than 10 blocks behind block explorer height $expectedHeight",
                expectedHeight - 10 < downloaderHeight.value
            )
        }
    }

    @Test
    @Ignore(
        "This test needs to be refactored to a separate test module. It causes SSLHandshakeException: Chain " +
            "validation failed on CI"
    )
    fun testSingleBlockDownload() = runBlocking {
        // Fetch height directly because the synchronizer hasn't started, yet. Then we test the
        // result, only if there is no server communication problem.
        val height = BlockHeight.new(wallet.network, 1_000_000)
        val block = runCatching {
            return@runCatching wallet.service.getBlockRange(height..height).first()
        }.onFailure {
            twig(it)
        }.getOrElse { return@runBlocking }

        runCatching {
            wallet.service.getLatestBlockHeight()
        }.getOrNull() ?: return@runBlocking
        assertTrue("$networkName failed to return a proper block. Height was ${block.height} but we expected $height", block.height == height.value)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun wallets() = listOf(
            // Testnet wallet
            arrayOf(
                TestWallet(TestWallet.Backups.SAMPLE_WALLET),
                "uviewtest1m3cyp6tdy3rewtpqazdxlsqkmu7xjedtqmp4da8mvxm87h4as38v5kz4ulw7x7nmgv5d8uwk743a5zt7aurtz2z2g74fu740ecp5fhdgakm6hgzr5jzcl75cmddlufmjpykrpkzj84yz8j5qe9c5935qt2tvd9dpx3m0zw5dwn3t2dtsdyqvy5jstf88w799qre549yyxw7dvk3murm3568ah6wqg5tdjka2ujtgct4q62hw7mfcxcyaeu8l6882hxkt9x4025mx3w35whcrmpxy8fqsh62esatczj8awxtrgnj8h2vj65r8595qt9jl4gz84w4mja74tymt8xxaguckeam",
                BlockHeight.new(ZcashNetwork.Testnet, 1330000)
            ),
            // Mainnet wallet
            arrayOf(
                TestWallet(TestWallet.Backups.SAMPLE_WALLET, ZcashNetwork.Mainnet),
                "uview1n8j8hckdh4rpxsa8qswmcv8mgu6g3f4l4se6ympej3qr6k5k5xlw47u02s3h2sy5aplkzuwysvum2p6weakvyc72udsuvplaq8r5jkw5h6cjfp26j8rudam7suzu6lwalzakpps2jv2x5v08gf3la02dtdlq75ca7k4urg6t0yncyly5wu26t6mfdfvxvhckr2qxzcwllnh947gn6wzg92f0mlhfds239q50gm4398n02anm23qgk8st49u0wmmw7flathr49h2twxvfm6gauasuq6z2fvs3t8g9ut4duk7tp7ry88dwacsutxzpwnm674y06mf3mz3tnu8s2fx4vatmcs9",
                BlockHeight.new(ZcashNetwork.Mainnet, 1000000)
            )
        )
    }
}
