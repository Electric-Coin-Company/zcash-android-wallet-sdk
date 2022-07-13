package cash.z.ecc.android.sdk.integration

import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import cash.z.ecc.android.sdk.annotation.MaintainedTest
import cash.z.ecc.android.sdk.annotation.TestPurpose
import cash.z.ecc.android.sdk.internal.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.util.TestWallet
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

/**
 * This test is intended to run to make sure that basic things are functional and pinpoint what is
 * not working. It was originally developed after a major refactor to find what broke.
 */
@MaintainedTest(TestPurpose.COMMIT)
@MediumTest
class SmokeTest {

    @Test
    fun testNotPlaintext() {
        val service =
            wallet.synchronizer.processor.downloader.lightWalletService as LightWalletGrpcService
        Assert.assertFalse(
            "Wallet is using plaintext. This will cause problems for the test. Ensure that the `lightwalletd_allow_very_insecure_connections` resource value is false",
            service.connectionInfo.usePlaintext
        )
    }

    @Test
    fun testFilePaths() {
        Assert.assertEquals("Invalid DataDB file", "/data/user/0/cash.z.ecc.android.sdk.test/databases/TestWallet_testnet_Data.db", wallet.initializer.rustBackend.pathDataDb)
        Assert.assertEquals("Invalid CacheDB file", "/data/user/0/cash.z.ecc.android.sdk.test/databases/TestWallet_testnet_Cache.db", wallet.initializer.rustBackend.pathCacheDb)
        Assert.assertEquals("Invalid CacheDB params dir", "/data/user/0/cash.z.ecc.android.sdk.test/cache/params", wallet.initializer.rustBackend.pathParamsDir)
    }

    @Test
    fun testBirthday() {
        Assert.assertEquals("Invalid birthday height", 1_320_000, wallet.initializer.checkpoint.height)
    }

    @Test
    fun testViewingKeys() {
        Assert.assertEquals("Invalid extfvk", "zxviewtestsapling1qv0ue89kqqqqpqqyt4cl5wvssx4wqq30e5m948p07dnwl9x3u75vvnzvjwwpjkrf8yk2gva0kkxk9p8suj4xawlzw9pajuxgap83wykvsuyzfrm33a2p2m4jz2205kgzx0l2lj2kyegtnuph6crkyvyjqmfxut84nu00wxgrstu5fy3eu49nzl8jzr4chmql4ysgg2t8htn9dtvxy8c7wx9rvcerqsjqm6lqln9syk3g8rr3xpy3l4nj0kawenzpcdtnv9qmy98vdhqzaf063", wallet.initializer.viewingKeys[0].extfvk)
        Assert.assertEquals("Invalid extpub", "0234965f30c8611253d035f44e68d4e2ce82150e8665c95f41ccbaf916b16c69d8", wallet.initializer.viewingKeys[0].extpub)
    }

    // This test takes an extremely long time
    // Does its runtime grow over time based on growth of the blockchain?
    @Test
    @LargeTest
    @Ignore("This test is extremely slow and times out before the timeout given")
    fun testSync() = runBlocking<Unit> {
        wallet.sync(300_000L)
    }

    companion object {
        val wallet = TestWallet(TestWallet.Backups.SAMPLE_WALLET)
    }
}
