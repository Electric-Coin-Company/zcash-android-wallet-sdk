package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.internal.Files
import cash.z.ecc.android.sdk.internal.ext.getDatabasePathSuspend
import cash.z.ecc.android.sdk.internal.ext.getNoBackupFilesDirCompat
import cash.z.ecc.android.sdk.test.getAppContext
import kotlinx.coroutines.runBlocking

object DatabasePathFixture {
    val NO_BACKUP_DIR_PATH: String = runBlocking {
        getAppContext().getNoBackupFilesDirCompat().absolutePath
    }
    val DATABASE_DIR_PATH: String = runBlocking {
        getAppContext().getDatabasePathSuspend("temporary.db").parentFile.let { parentFile ->
            assert(parentFile != null) { "Failed to create database folder." }
            parentFile!!.mkdirs()

            assert(parentFile.exists()) { "Failed to check database folder." }
            parentFile.absolutePath
        }
    }
    const val INTERNAL_DATABASE_PATH = Files.NO_BACKUP_SUBDIRECTORY

    internal fun new(
        baseFolderPath: String = NO_BACKUP_DIR_PATH,
        internalPath: String = INTERNAL_DATABASE_PATH
    ) = "$baseFolderPath/$internalPath"
}
