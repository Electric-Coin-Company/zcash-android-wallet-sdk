package cash.z.ecc.android.sdk.integration

import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.DefaultSynchronizerFactory
import cash.z.ecc.android.sdk.internal.SaplingParamTool
import cash.z.ecc.android.sdk.internal.db.DatabaseCoordinator
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.util.TestWallet
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class SynchronizerFactoryTest {

    @Test
    @SmallTest
    fun testFilePaths() {
        val rustBackend = runBlocking {
            val coordinator = DatabaseCoordinator.getInstance(ApplicationProvider.getApplicationContext())
            DefaultSynchronizerFactory.defaultRustBackend(
                ZcashNetwork.Testnet,
                "TestWallet",
                TestWallet.Backups.SAMPLE_WALLET.testnetBirthday,
                SaplingParamTool.new(ApplicationProvider.getApplicationContext()),
                coordinator
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
            rustBackend.fsBlockDbRoot.absolutePath.endsWith(
                "no_backup/co.electricoin.zcash/TestWallet_testnet_${DatabaseCoordinator.DB_FS_BLOCK_DB_ROOT_NAME}"
            )
        )
        assertTrue(
            "Invalid CacheDB params dir",
            rustBackend.saplingParamDir.endsWith(
                "no_backup/co.electricoin.zcash"
            )
        )
    }
}
