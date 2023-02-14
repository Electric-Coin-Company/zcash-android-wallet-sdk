package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.internal.db.DatabaseCoordinator
import cash.z.ecc.android.sdk.model.ZcashNetwork

/**
 * Provides a unified way for getting a fixture root name for database cache files for test purposes.
 */
object DatabaseCacheFilesRootFixture {
    const val TEST_CACHE_ROOT_NAME = DatabaseCoordinator.DB_FS_CACHE_NAME
    const val TEST_CACHE_ROOT_NAME_ALIAS = "zcash_sdk"
    val TEST_NETWORK = ZcashNetwork.Testnet

    internal fun newCacheRoot(
        name: String = TEST_CACHE_ROOT_NAME,
        alias: String = TEST_CACHE_ROOT_NAME_ALIAS,
        network: String = TEST_NETWORK.networkName
    ) = "${alias}_${network}_$name"
}
