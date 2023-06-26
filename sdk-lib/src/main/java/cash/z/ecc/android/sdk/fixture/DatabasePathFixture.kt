package cash.z.ecc.android.sdk.fixture

import android.content.Context
import cash.z.ecc.android.sdk.internal.Files
import cash.z.ecc.android.sdk.internal.ext.getNoBackupFilesDirSuspend
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Provides a unified way for getting fixture directories on the database root path for test purposes.
 */
object DatabasePathFixture {

    const val INTERNAL_DATABASE_PATH = Files.NO_BACKUP_SUBDIRECTORY

    internal fun new(
        context: Context,
        internalPath: String = INTERNAL_DATABASE_PATH
    ): String {
        val baseFolderPath = runBlocking {
            context.getNoBackupFilesDirSuspend().absolutePath
        }
        return File(baseFolderPath, internalPath).absolutePath
    }
}
