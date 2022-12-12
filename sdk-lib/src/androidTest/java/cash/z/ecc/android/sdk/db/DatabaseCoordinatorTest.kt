package cash.z.ecc.android.sdk.db

import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.internal.db.DatabaseCoordinator
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.test.getAppContext
import cash.z.ecc.fixture.DatabaseNameFixture
import cash.z.ecc.fixture.DatabasePathFixture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

    private data class Triple(
        var firstJobValidity: Boolean,
        var secondJobValidity: Boolean,
        var resultFile: File?
    )

    /**
     * Sanity check of the database coordinator instance and its thread-safe implementation. Our aim here is to run
     * two jobs in parallel (the second one runs immediately after the first was started) to test mutex
     * implementation and correct DatabaseCoordinator function call result.
     *
     * We use CoroutineStart.UNDISPATCHED to immediately run the first created job. We use runBlocking instead of
     * runTest to be able to provide a tiny delay between the two jobs or to return the block result to stress test.
     * Our goal here is to run these two jobs in parallel, and also to have the first one access the shared resource
     * (DatabaseCoordinator) first. We add checks to ensure that these two jobs really run together and to avoid
     * theirs serialization. If any of these conditions do not succeed we claim the test iteration as invalid and the
     * test returns false.
     *
     * If the conditions succeed we approach to check if the second job changed the shared resource correctly.
     *
     * @return true in case of job conditions succeed and false otherwise
     */
    private fun mutex_test_iteration(): Boolean = runBlocking {
        val testResult = Triple(
            firstJobValidity = true,
            secondJobValidity = true,
            resultFile = null
        )

        val firstJob = launch(start = CoroutineStart.UNDISPATCHED) {
            val firstResultFile = dbCoordinator.cacheDbFile(
                DatabaseNameFixture.TEST_DB_NETWORK,
                DatabaseNameFixture.TEST_DB_ALIAS
            )
            // Check if the second job hasn't finished first, and thus makes this test invalid
            if (testResult.resultFile != null) {
                testResult.firstJobValidity = false
                return@launch
            }
            testResult.resultFile = firstResultFile
        }

        // Small delay to support the need for running the second job right after the first one starts. Note this
        // works well on local emulators, but not on CI emulators.
        // delay(1)

        val secondJob = launch(start = CoroutineStart.UNDISPATCHED) {
            // We check here if the first job is still running and its result is not already proceeded
            if (!firstJob.isActive || testResult.resultFile != null) {
                testResult.secondJobValidity = false
                return@launch
            }
            testResult.secondJobValidity = true
            testResult.resultFile = dbCoordinator.cacheDbFile(
                ZcashNetwork.Mainnet,
                "TestZcashSdk"
            )
        }

        // Wait until both jobs are done
        joinAll(firstJob, secondJob)

        // Check validity of this test iteration
        if (!testResult.firstJobValidity ||
            !testResult.secondJobValidity
        ) {
            return@runBlocking false
        }

        // And here we assert that the second job was the one which waited, and thus accessed as second and changed
        // the shared result correctly
        assertTrue(testResult.resultFile != null)
        assertTrue(testResult.resultFile!!.absolutePath.isNotEmpty())
        assertTrue(testResult.resultFile!!.absolutePath.contains(ZcashNetwork.Mainnet.networkName))
        assertTrue(testResult.resultFile!!.absolutePath.contains("TestZcashSdk"))

        return@runBlocking true
    }

    /**
     * This stress test runs its subtest repeatedly and counts its failed iterations. If it exceeds the allowed fail
     * threshold, then we claim this test as unsuccessful.
     *
     * According to manual testing, the valid tests ratio is around 90-99% when running on local emulators or
     * physical devices. But on CI we get about 60-70% valid tests ratio.
     */
    @FlakyTest
    @Test
    @MediumTest
    fun mutex_stress_test() {
        val allAttempts = 100
        var failedAttempts = 0
        val validAttemptsRatio = 0.25f

        // We run the mutex test multiple times sequentially to catch a possible problem
        for (x in 1..allAttempts) {
            if (!mutex_test_iteration()) {
                failedAttempts++
            }
        }

        val printResult = "${allAttempts - failedAttempts}/$allAttempts"

        assertTrue(
            "Failed on insufficient valid attempts: $printResult",
            failedAttempts < (allAttempts - (allAttempts * validAttemptsRatio))
        )
    }

    @Test
    @SmallTest
    @OptIn(ExperimentalCoroutinesApi::class)
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
    @OptIn(ExperimentalCoroutinesApi::class)
    fun database_files_move_test() = runTest {
        val parentFile = File(
            DatabasePathFixture.new(
                baseFolderPath = DatabasePathFixture.DATABASE_DIR_PATH,
                internalPath = ""
            )
        )

        val originalDbFile = getEmptyFile(
            parent = parentFile,
            fileName = DatabaseNameFixture.newDb(
                name = DatabaseCoordinator.DB_CACHE_NAME_LEGACY,
                alias = DatabaseCoordinator.ALIAS_LEGACY
            )
        )

        val originalDbJournalFile = getEmptyFile(
            parent = parentFile,
            fileName = DatabaseNameFixture.newDbJournal(
                name = DatabaseCoordinator.DB_CACHE_NAME_LEGACY,
                alias = DatabaseCoordinator.ALIAS_LEGACY
            )
        )

        val originalDbWalFile = getEmptyFile(
            parent = parentFile,
            fileName = DatabaseNameFixture.newDbWal(
                name = DatabaseCoordinator.DB_CACHE_NAME_LEGACY,
                alias = DatabaseCoordinator.ALIAS_LEGACY
            )
        )

        val expectedDbFile = File(
            DatabasePathFixture.new(),
            DatabaseNameFixture.newDb(name = DatabaseCoordinator.DB_CACHE_NAME)
        )
        val expectedDbJournalFile = File(
            DatabasePathFixture.new(),
            DatabaseNameFixture.newDbJournal(name = DatabaseCoordinator.DB_CACHE_NAME)
        )
        val expectedDbWalFile = File(
            DatabasePathFixture.new(),
            DatabaseNameFixture.newDbWal(name = DatabaseCoordinator.DB_CACHE_NAME)
        )

        assertTrue(originalDbFile.existsSuspend())
        assertTrue(originalDbJournalFile.existsSuspend())
        assertTrue(originalDbWalFile.existsSuspend())

        assertFalse(expectedDbFile.existsSuspend())
        assertFalse(expectedDbJournalFile.existsSuspend())
        assertFalse(expectedDbWalFile.existsSuspend())

        dbCoordinator.cacheDbFile(
            DatabaseNameFixture.TEST_DB_NETWORK,
            DatabaseNameFixture.TEST_DB_ALIAS
        ).also { resultFile ->
            assertTrue(resultFile.existsSuspend())
            assertEquals(expectedDbFile.absolutePath, resultFile.absolutePath)

            assertTrue(expectedDbFile.existsSuspend())
            assertTrue(expectedDbJournalFile.existsSuspend())
            assertTrue(expectedDbWalFile.existsSuspend())

            assertFalse(originalDbFile.existsSuspend())
            assertFalse(originalDbJournalFile.existsSuspend())
            assertFalse(originalDbWalFile.existsSuspend())
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
    @OptIn(ExperimentalCoroutinesApi::class)
    fun delete_database_files_test() = runTest {
        val parentFile = File(
            DatabasePathFixture.new(
                baseFolderPath = DatabasePathFixture.NO_BACKUP_DIR_PATH,
                internalPath = DatabasePathFixture.INTERNAL_DATABASE_PATH
            )
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

        assertTrue(dbFile.existsSuspend())
        assertTrue(dbJournalFile.existsSuspend())
        assertTrue(dbWalFile.existsSuspend())

        dbCoordinator.deleteDatabases(DatabaseNameFixture.TEST_DB_NETWORK, DatabaseNameFixture.TEST_DB_ALIAS).also {
            assertFalse(dbFile.existsSuspend())
            assertFalse(dbJournalFile.existsSuspend())
            assertFalse(dbWalFile.existsSuspend())
        }
    }
}
