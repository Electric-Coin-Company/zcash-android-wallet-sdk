package cash.z.ecc.android.sdk.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.internal.db.DatabaseCoordinator
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.test.getAppContext
import cash.z.ecc.fixture.DatabaseCacheFilesRootFixture
import cash.z.ecc.fixture.DatabaseNameFixture
import cash.z.ecc.fixture.DatabasePathFixture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class DatabaseCoordinatorTest {

    private val dbCoordinator = DatabaseCoordinator.getInstance(getAppContext())

    @Before
    fun clear_test_files() {
        val databaseDir = DatabasePathFixture.new(baseFolderPath = DatabasePathFixture.DATABASE_DIR_PATH)
        val noBackupDir = DatabasePathFixture.new(baseFolderPath = DatabasePathFixture.NO_BACKUP_DIR_PATH)
        File(databaseDir).deleteRecursively()
        File(noBackupDir).deleteRecursively()
    }

    @Test
    @SmallTest
    @OptIn(ExperimentalCoroutinesApi::class)
    fun database_cache_root_directory_creation_test() = runTest {
        val parentDirectory = File(DatabasePathFixture.new())
        val destinationDirectory = DatabaseCacheFilesRootFixture.newCacheRoot()
        val expectedDirectoryPath = File(parentDirectory, destinationDirectory).path

        dbCoordinator.fsBlockDbRoot(
            DatabaseNameFixture.TEST_DB_NETWORK,
            DatabaseNameFixture.TEST_DB_ALIAS
        ).also { resultFile ->
            assertEquals(expectedDirectoryPath, resultFile.absolutePath)
        }
    }

    @Test
    @SmallTest
    @OptIn(ExperimentalCoroutinesApi::class)
    fun database_data_file_creation_test() = runTest {
        val directory = File(DatabasePathFixture.new())
        val fileName = DatabaseNameFixture.newDb(name = DatabaseCoordinator.DB_DATA_NAME)
        val expectedFilePath = File(directory, fileName).path

        dbCoordinator.dataDbFile(
            DatabaseNameFixture.TEST_DB_NETWORK,
            DatabaseNameFixture.TEST_DB_ALIAS
        ).also { resultFile ->
            assertEquals(expectedFilePath, resultFile.absolutePath)
        }
    }

    @Test
    @SmallTest
    @OptIn(ExperimentalCoroutinesApi::class)
    fun database_transactions_file_creation_test() = runTest {
        val directory = File(DatabasePathFixture.new())
        val fileName = DatabaseNameFixture.newDb(name = DatabaseCoordinator.DB_PENDING_TRANSACTIONS_NAME)
        val expectedFilePath = File(directory, fileName).path

        dbCoordinator.pendingTransactionsDbFile(
            DatabaseNameFixture.TEST_DB_NETWORK,
            DatabaseNameFixture.TEST_DB_ALIAS
        ).also { resultFile ->
            assertEquals(expectedFilePath, resultFile.absolutePath)
        }
    }

    @Test
    @SmallTest
    @Suppress("LongMethod")
    fun data_database_files_move_test() = runTest {
        val parentFile = File(
            DatabasePathFixture.new(
                baseFolderPath = DatabasePathFixture.DATABASE_DIR_PATH,
                internalPath = ""
            )
        )

        val originalDbFile = getEmptyFile(
            parent = parentFile,
            fileName = DatabaseNameFixture.newDb(
                name = DatabaseCoordinator.DB_DATA_NAME_LEGACY,
                alias = DatabaseCoordinator.ALIAS_LEGACY
            )
        )

        val originalDbJournalFile = getEmptyFile(
            parent = parentFile,
            fileName = DatabaseNameFixture.newDbJournal(
                name = DatabaseCoordinator.DB_DATA_NAME_LEGACY,
                alias = DatabaseCoordinator.ALIAS_LEGACY
            )
        )

        val originalDbWalFile = getEmptyFile(
            parent = parentFile,
            fileName = DatabaseNameFixture.newDbWal(
                name = DatabaseCoordinator.DB_DATA_NAME_LEGACY,
                alias = DatabaseCoordinator.ALIAS_LEGACY
            )
        )

        val expectedDbFile = File(
            DatabasePathFixture.new(),
            DatabaseNameFixture.newDb(name = DatabaseCoordinator.DB_DATA_NAME)
        )
        val expectedDbJournalFile = File(
            DatabasePathFixture.new(),
            DatabaseNameFixture.newDbJournal(name = DatabaseCoordinator.DB_DATA_NAME)
        )
        val expectedDbWalFile = File(
            DatabasePathFixture.new(),
            DatabaseNameFixture.newDbWal(name = DatabaseCoordinator.DB_DATA_NAME)
        )

        assertTrue(originalDbFile.exists())
        assertTrue(originalDbJournalFile.exists())
        assertTrue(originalDbWalFile.exists())

        assertFalse(expectedDbFile.exists())
        assertFalse(expectedDbJournalFile.exists())
        assertFalse(expectedDbWalFile.exists())

        dbCoordinator.dataDbFile(
            DatabaseNameFixture.TEST_DB_NETWORK,
            DatabaseNameFixture.TEST_DB_ALIAS
        ).also { resultFile ->
            assertTrue(resultFile.exists())
            assertEquals(expectedDbFile.absolutePath, resultFile.absolutePath)

            assertTrue(expectedDbFile.exists())
            assertTrue(expectedDbJournalFile.exists())
            assertTrue(expectedDbWalFile.exists())

            assertFalse(originalDbFile.exists())
            assertFalse(originalDbJournalFile.exists())
            assertFalse(originalDbWalFile.exists())
        }
    }

    private fun getEmptyFile(parent: File, fileName: String): File {
        return File(parent, fileName).apply {
            assertTrue(parentFile != null)
            parentFile!!.mkdirs()
            assertTrue(parentFile!!.exists())

            createNewFile()
            assertTrue(exists())
        }
    }

    @Test
    @SmallTest
    fun delete_data_database_files_test() = runTest {
        val parentFile = File(
            DatabasePathFixture.new(
                baseFolderPath = DatabasePathFixture.NO_BACKUP_DIR_PATH,
                internalPath = DatabasePathFixture.INTERNAL_DATABASE_PATH
            )
        )

        val dbFile = getEmptyFile(
            parent = parentFile,
            fileName = DatabaseNameFixture.newDb(name = DatabaseCoordinator.DB_DATA_NAME)
        )

        val dbJournalFile = getEmptyFile(
            parent = parentFile,
            fileName = DatabaseNameFixture.newDbJournal(name = DatabaseCoordinator.DB_DATA_NAME)
        )

        val dbWalFile = getEmptyFile(
            parent = parentFile,
            fileName = DatabaseNameFixture.newDbWal(name = DatabaseCoordinator.DB_DATA_NAME)
        )

        assertTrue(dbFile.exists())
        assertTrue(dbJournalFile.exists())
        assertTrue(dbWalFile.exists())

        dbCoordinator.deleteDatabases(DatabaseNameFixture.TEST_DB_NETWORK, DatabaseNameFixture.TEST_DB_ALIAS).also {
            assertFalse(dbFile.exists())
            assertFalse(dbJournalFile.exists())
            assertFalse(dbWalFile.exists())
        }
    }

    /**
     * Note that this situation is just hypothetical, as the legacy database files should be placed only on one of
     * the legacy locations, not both, but it is alright to test it together.
     */
    @Test
    @SmallTest
    @Suppress("LongMethod")
    fun delete_all_legacy_database_files_test() = runTest {
        // create older location legacy files
        val olderLegacyParentFile = File(
            DatabasePathFixture.new(
                baseFolderPath = DatabasePathFixture.DATABASE_DIR_PATH,
                internalPath = ""
            )
        )

        val olderLegacyDbFile = getEmptyFile(
            parent = olderLegacyParentFile,
            fileName = DatabaseNameFixture.newDb(
                name = DatabaseCoordinator.DB_CACHE_OLDER_NAME_LEGACY,
                network = DatabaseNameFixture.TEST_DB_NETWORK.networkName,
                alias = DatabaseCoordinator.ALIAS_LEGACY
            )
        )

        val olderLegacyDbJournalFile = getEmptyFile(
            parent = olderLegacyParentFile,
            fileName = DatabaseNameFixture.newDbJournal(
                name = DatabaseCoordinator.DB_CACHE_OLDER_NAME_LEGACY,
                network = DatabaseNameFixture.TEST_DB_NETWORK.networkName,
                alias = DatabaseCoordinator.ALIAS_LEGACY
            )
        )

        val olderLegacyDbWalFile = getEmptyFile(
            parent = olderLegacyParentFile,
            fileName = DatabaseNameFixture.newDbWal(
                name = DatabaseCoordinator.DB_CACHE_OLDER_NAME_LEGACY,
                network = DatabaseNameFixture.TEST_DB_NETWORK.networkName,
                alias = DatabaseCoordinator.ALIAS_LEGACY
            )
        )

        // create newer location legacy files
        val newerLegacyParentFile = File(
            DatabasePathFixture.new(
                baseFolderPath = DatabasePathFixture.NO_BACKUP_DIR_PATH,
                internalPath = DatabasePathFixture.INTERNAL_DATABASE_PATH
            )
        )

        val newerLegacyDbFile = getEmptyFile(
            parent = newerLegacyParentFile,
            fileName = DatabaseNameFixture.newDb(
                name = DatabaseCoordinator.DB_CACHE_NEWER_NAME_LEGACY,
                network = DatabaseNameFixture.TEST_DB_NETWORK.networkName,
                alias = DatabaseNameFixture.TEST_DB_ALIAS
            )
        )

        val newerLegacyDbJournalFile = getEmptyFile(
            parent = newerLegacyParentFile,
            fileName = DatabaseNameFixture.newDbJournal(
                name = DatabaseCoordinator.DB_CACHE_NEWER_NAME_LEGACY,
                network = DatabaseNameFixture.TEST_DB_NETWORK.networkName,
                alias = DatabaseNameFixture.TEST_DB_ALIAS
            )
        )

        val newerLegacyDbWalFile = getEmptyFile(
            parent = newerLegacyParentFile,
            fileName = DatabaseNameFixture.newDbWal(
                name = DatabaseCoordinator.DB_CACHE_NEWER_NAME_LEGACY,
                network = DatabaseNameFixture.TEST_DB_NETWORK.networkName,
                alias = DatabaseNameFixture.TEST_DB_ALIAS
            )
        )

        // check all files in place
        assertTrue(olderLegacyDbFile.exists())
        assertTrue(olderLegacyDbJournalFile.exists())
        assertTrue(olderLegacyDbWalFile.exists())

        assertTrue(newerLegacyDbFile.exists())
        assertTrue(newerLegacyDbJournalFile.exists())
        assertTrue(newerLegacyDbWalFile.exists())

        // once we access the latest file system blocks storage root directory, all the legacy database files should
        // be removed
        dbCoordinator.fsBlockDbRoot(
            network = DatabaseNameFixture.TEST_DB_NETWORK,
            alias = DatabaseNameFixture.TEST_DB_ALIAS
        ).also {
            assertFalse(olderLegacyDbFile.exists())
            assertFalse(olderLegacyDbJournalFile.exists())
            assertFalse(olderLegacyDbWalFile.exists())

            assertFalse(newerLegacyDbFile.exists())
            assertFalse(newerLegacyDbJournalFile.exists())
            assertFalse(newerLegacyDbWalFile.exists())
        }
    }

    @Test
    @SmallTest
    fun data_db_path() = runTest {
        val coordinator = DatabaseCoordinator.getInstance(ApplicationProvider.getApplicationContext())
        val dataDbFile = coordinator.dataDbFile(ZcashNetwork.Testnet, "TestWallet")
        assertTrue(
            "Invalid DataDB file",
            dataDbFile.absolutePath.endsWith(
                "no_backup/co.electricoin.zcash/TestWallet_testnet_${DatabaseCoordinator.DB_DATA_NAME}"
            )
        )
    }

    @Test
    @SmallTest
    fun cache_path() = runTest {
        val coordinator = DatabaseCoordinator.getInstance(ApplicationProvider.getApplicationContext())
        val cache = coordinator.fsBlockDbRoot(ZcashNetwork.Testnet, "TestWallet")
        assertTrue(
            "Invalid CacheDB file",
            cache.absolutePath.endsWith(
                "no_backup/co.electricoin.zcash/TestWallet_testnet_${DatabaseCoordinator.DB_FS_BLOCK_DB_ROOT_NAME}"
            )
        )
    }
}
