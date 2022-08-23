package cash.z.ecc.android.sdk.integration

import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import cash.z.ecc.android.sdk.annotation.MaintainedTest
import cash.z.ecc.android.sdk.annotation.TestPurpose
import cash.z.ecc.android.sdk.db.DatabaseCoordinator
import cash.z.ecc.android.sdk.util.TestWallet
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
        assertTrue(
            "Invalid DataDB file",
            wallet.initializer.rustBackend.dataDbFile.absolutePath.endsWith(
                "no_backup/co.electricoin.zcash/TestWallet_testnet_${DatabaseCoordinator.DB_DATA_NAME}"
            )
        )
        assertTrue(
            "Invalid CacheDB file",
            wallet.initializer.rustBackend.cacheDbFile.absolutePath.endsWith(
                "no_backup/co.electricoin.zcash/TestWallet_testnet_${DatabaseCoordinator.DB_CACHE_NAME}"
            )
        )
        assertTrue(
            "Invalid CacheDB params dir",
            wallet.initializer.rustBackend.pathParamsDir.endsWith("cache/params")
        )
    }

    @Test
    fun testBirthday() {
        assertEquals(
            "Invalid birthday height",
            1_330_000,
            wallet.initializer.checkpoint.height.value
        )
    }

    @Test
    fun testViewingKeys() {
        assertEquals("Invalid encoding", "uviewtest1m3cyp6tdy3rewtpqazdxlsqkmu7xjedtqmp4da8mvxm87h4as38v5kz4ulw7x7nmgv5d8uwk743a5zt7aurtz2z2g74fu740ecp5fhdgakm6hgzr5jzcl75cmddlufmjpykrpkzj84yz8j5qe9c5935qt2tvd9dpx3m0zw5dwn3t2dtsdyqvy5jstf88w799qre549yyxw7dvk3murm3568ah6wqg5tdjka2ujtgct4q62hw7mfcxcyaeu8l6882hxkt9x4025mx3w35whcrmpxy8fqsh62esatczj8awxtrgnj8h2vj65r8595qt9jl4gz84w4mja74tymt8xxaguckeam", wallet.initializer.viewingKeys[0].encoding)
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
