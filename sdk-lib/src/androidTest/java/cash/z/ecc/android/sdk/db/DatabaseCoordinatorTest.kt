package cash.z.ecc.android.sdk.db

import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.test.getAppContext
import cash.z.ecc.android.sdk.type.ZcashNetwork
import cash.z.ecc.fixture.DatabaseNameFixture
import cash.z.ecc.fixture.DatabasePathFixture
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
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

    // Sanity check of the database coordinator instance and its thread-safe implementation. Our aim
    // here is to run two jobs in parallel (the second one runs immediately after the first was started)
    // to test mutex implementation and correct DatabaseCoordinator function call result.
    @Test
    @SmallTest
    fun mutex_test() = runTest {
        var testResult: File? = null

        launch {
            delay(1000)
            testResult = dbCoordinator.cacheDbFile(
                DatabaseNameFixture.TEST_DB_NETWORK,
                DatabaseNameFixture.TEST_DB_ALIAS
            )
        }
        val job2 = launch {
            delay(1001)
            testResult = dbCoordinator.cacheDbFile(
                ZcashNetwork.Mainnet,
                "TestZcashSdk"
            )
        }

        advanceTimeBy(1002)

        job2.join().also {
            assertTrue(testResult != null)
            assertTrue(testResult!!.absolutePath.isNotEmpty())
            assertTrue(testResult!!.absolutePath.contains(ZcashNetwork.Mainnet.networkName))
            assertTrue(testResult!!.absolutePath.contains("TestZcashSdk"))
        }
    }

    @Test
    @MediumTest
    fun mutex_stress_test() {
        // We run the mutex test multiple times sequentially to catch a possible problem.
        for (x in 0..50) {
            mutex_test()
        }
    }

    @Test
    @SmallTest
    fun database_cache_file_creation_test() = runTest {
        val directory = File(DatabasePathFixture.new())
        val fileName = DatabaseNameFixture.newDb(name = DatabaseCoordinator.DB_CACHE_NAME)
        val expectedFilePath = File(directory, fileName).path

        dbCoordinator.cacheDbFile(
            DatabaseNameFixture.TEST_DB_NETWORK,
            DatabaseNameFixture.TEST_DB_ALIAS
        ).also { resultFile ->
            assertEquals(expectedFilePath, resultFile.absolutePath)
        }
    }

    @Test
    @SmallTest
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
    fun database_transactions_file_creation_test() = runTest {
        val directory = File(DatabasePathFixture.new())
        val fileName = DatabaseNameFixture.newDb(name = DatabaseCoordinator.DB_PENDING_TRANSACTIONS_NAME)
        val expectedFilePath = File(directory, fileName).path

        dbCoordinator.pendingTransactionsDbPath(
            DatabaseNameFixture.TEST_DB_NETWORK,
            DatabaseNameFixture.TEST_DB_ALIAS
        ).also { resultFile ->
            assertEquals(expectedFilePath, resultFile.absolutePath)
        }
    }

    @Test
    @SmallTest
    fun database_files_move_test() = runTest {
        val parentFile = File(DatabasePathFixture.new(
            baseFolderPath = DatabasePathFixture.DATABASE_DIR_PATH,
            internalPath = "")
        )

        val originalDbFile = getEmptyFile(
            parent = parentFile,
            fileName = DatabaseNameFixture.newDb(name = DatabaseCoordinator.DB_CACHE_NAME)
        )

        val originalDbJournalFile = getEmptyFile(
            parent = parentFile,
            fileName = DatabaseNameFixture.newDbJournal(name = DatabaseCoordinator.DB_CACHE_NAME)
        )

        val originalDbWalFile = getEmptyFile(
            parent = parentFile,
            fileName = DatabaseNameFixture.newDbWal(name = DatabaseCoordinator.DB_CACHE_NAME)
        )

        val expectedDbFile = File(
            DatabasePathFixture.new(), DatabaseNameFixture.newDb(name = DatabaseCoordinator.DB_CACHE_NAME)
        )
        val expectedDbJournalFile = File(
            DatabasePathFixture.new(), DatabaseNameFixture.newDbJournal(name = DatabaseCoordinator.DB_CACHE_NAME)
        )
        val expectedDbWalFile = File(
            DatabasePathFixture.new(), DatabaseNameFixture.newDbWal(name = DatabaseCoordinator.DB_CACHE_NAME)
        )

        dbCoordinator.cacheDbFile(
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
    fun delete_database_files_test() = runTest {
        val parentFile = File(DatabasePathFixture.new(
            baseFolderPath = DatabasePathFixture.DATABASE_DIR_PATH,
            internalPath = "")
        )

        val dbFile = getEmptyFile(
            parent = parentFile,
            fileName = DatabaseNameFixture.newDb(name = DatabaseCoordinator.DB_CACHE_NAME)
        )

        val dbJournalFile = getEmptyFile(
            parent = parentFile,
            fileName = DatabaseNameFixture.newDbJournal(name = DatabaseCoordinator.DB_CACHE_NAME)
        )

        val dbWalFile = getEmptyFile(
            parent = parentFile,
            fileName = DatabaseNameFixture.newDbWal(name = DatabaseCoordinator.DB_CACHE_NAME)
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
}
