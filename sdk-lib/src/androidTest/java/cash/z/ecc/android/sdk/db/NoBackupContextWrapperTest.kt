package cash.z.ecc.android.sdk.db

import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.internal.NoBackupContextWrapper
import cash.z.ecc.android.sdk.test.getAppContext
import cash.z.ecc.fixture.DatabaseNameFixture
import cash.z.ecc.fixture.DatabasePathFixture
import org.junit.Test

class NoBackupContextWrapperTest {

    private val noBackupContextWrapper = NoBackupContextWrapper(getAppContext())

    @Test
    @SmallTest
    fun get_context_test() {
        assert(noBackupContextWrapper.applicationContext is NoBackupContextWrapper)
        assert(noBackupContextWrapper.baseContext is NoBackupContextWrapper)
    }

    @Test
    @SmallTest
    fun get_database_path_test() {
        val testDbPath = "${DatabasePathFixture.new()}/${DatabaseNameFixture.newDb()}"
        val testDbFile = noBackupContextWrapper.getDatabasePath(testDbPath)

        testDbFile.absolutePath.also {
            assert(it.isNotEmpty())
            assert(it.contains(DatabaseNameFixture.TEST_DB_NAME))
            assert(it.contains(DatabasePathFixture.NO_BACKUP_DIR_PATH))
            assert(it.contains(DatabasePathFixture.INTERNAL_DATABASE_PATH))
        }
    }
}
