package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.db.DatabaseCoordinator
import cash.z.ecc.android.sdk.type.ZcashNetwork

object DatabaseNameFixture {
    const val TEST_DB_NAME = "empty.db"
    const val TEST_DB_JOURNAL_NAME_SUFFIX = DatabaseCoordinator.DATABASE_FILE_JOURNAL_SUFFIX
    const val TEST_DB_WAL_NAME_SUFFIX = DatabaseCoordinator.DATABASE_FILE_WAL_SUFFIX

    const val TEST_DB_ALIAS = "ZcashSdk"
    val TEST_DB_NETWORK = ZcashNetwork.Testnet

    internal fun newDb(
        name: String = TEST_DB_NAME,
        alias: String = TEST_DB_ALIAS,
        network: String = TEST_DB_NETWORK.networkName
    ) = "${alias}_${network}_$name"

    internal fun newDbJournal(
        name: String = TEST_DB_NAME,
        alias: String = TEST_DB_ALIAS,
        network: String = TEST_DB_NETWORK.networkName
    ) = "${alias}_${network}_$name-$TEST_DB_JOURNAL_NAME_SUFFIX"

    internal fun newDbWal(
        name: String = TEST_DB_NAME,
        alias: String = TEST_DB_ALIAS,
        network: String = TEST_DB_NETWORK.networkName
    ) = "${alias}_${network}_$name-$TEST_DB_WAL_NAME_SUFFIX"
}
