package cash.z.ecc.android.sdk.internal.ext

import android.content.Context
import androidx.core.content.ContextCompat
import cash.z.ecc.android.sdk.internal.AndroidApiVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal suspend fun Context.getDatabasePathSuspend(fileName: String) =
    withContext(Dispatchers.IO) { getDatabasePath(fileName) }

internal suspend fun Context.getNoBackupFilesDirSuspend() =
    withContext(Dispatchers.IO) { noBackupFilesDir }

internal suspend fun Context.getCacheDirSuspend() =
    withContext(Dispatchers.IO) { cacheDir }

internal suspend fun Context.getFilesDirSuspend() =
    withContext(Dispatchers.IO) { filesDir }

internal suspend fun Context.getDataDirCompatSuspend() =
    withContext(Dispatchers.IO) { ContextCompat.getDataDir(this@getDataDirCompatSuspend) }

private const val FAKE_NO_BACKUP_FOLDER = "no_backup" // $NON-NLS

/**
 * @return Path to the no backup folder, with fallback behavior for API < 21.
 */
internal suspend fun Context.getNoBackupFilesDirCompat(): File {
    val dir = if (AndroidApiVersion.isAtLeastL) {
        getNoBackupFilesDirSuspend()
    } else {
        File(getDataDirCompatSuspend(), FAKE_NO_BACKUP_FOLDER)
    }

    if (!dir.existsSuspend()) {
        if (!dir.mkdirsSuspend()) {
            error("no_backup directory does not exist and could not be created")
        }
    }

    if (!dir.canWriteSuspend()) {
        error("${dir.absolutePath} directory is not writable")
    }

    return dir
}
