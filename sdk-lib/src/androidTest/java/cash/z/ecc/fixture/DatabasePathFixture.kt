package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.internal.Files
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.getDatabasePathSuspend
import cash.z.ecc.android.sdk.internal.ext.getNoBackupFilesDirSuspend
import cash.z.ecc.android.sdk.test.getAppContext
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Provides a unified way for getting fixture directories on the database root path for test purposes.
 */
object DatabasePathFixture {
    val NO_BACKUP_DIR_PATH: String = runBlocking {
        getAppContext().getNoBackupFilesDirSuspend().absolutePath
    }
    val DATABASE_DIR_PATH: String = runBlocking {
        getAppContext().getDatabasePathSuspend("temporary.db").parentFile.let { parentFile ->
            assert(parentFile != null) { "Failed to create database folder." }
            parentFile!!.mkdirs()

            assert(parentFile.existsSuspend()) { "Failed to check database folder." }
            parentFile.absolutePath
        }
    }
    const val INTERNAL_DATABASE_PATH = Files.NO_BACKUP_SUBDIRECTORY

    internal fun new(
        baseFolderPath: String = NO_BACKUP_DIR_PATH,
        internalPath: String = INTERNAL_DATABASE_PATH
    ) = File(baseFolderPath, internalPath).absolutePath
}
