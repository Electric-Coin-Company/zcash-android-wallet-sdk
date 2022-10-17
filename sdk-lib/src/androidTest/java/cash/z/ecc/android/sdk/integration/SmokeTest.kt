package cash.z.ecc.android.sdk.integration

import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import cash.z.ecc.android.sdk.DefaultSynchronizerFactory
import cash.z.ecc.android.sdk.annotation.MaintainedTest
import cash.z.ecc.android.sdk.annotation.TestPurpose
import cash.z.ecc.android.sdk.db.DatabaseCoordinator
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.util.TestWallet
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
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
            "Invalid DataDB file",
            rustBackend.dataDbFile.absolutePath.endsWith(
                "no_backup/co.electricoin.zcash/TestWallet_testnet_${DatabaseCoordinator.DB_DATA_NAME}"
            )
        )
        assertTrue(
            "Invalid CacheDB file",
            rustBackend.cacheDbFile.absolutePath.endsWith(
                "no_backup/co.electricoin.zcash/TestWallet_testnet_${DatabaseCoordinator.DB_CACHE_NAME}"
            )
        )
        assertTrue(
            "Invalid CacheDB params dir",
            rustBackend.saplingParamDir.endsWith(
                "no_backup/co.electricoin.zcash"
            )
        )
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
