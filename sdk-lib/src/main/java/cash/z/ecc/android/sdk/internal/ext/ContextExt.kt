package cash.z.ecc.android.sdk.internal.ext

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun Context.getDatabasePathSuspend(fileName: String) =
    withContext(Dispatchers.IO) { getDatabasePath(fileName) }

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
suspend fun Context.getNoBackupPathSuspend() =
    withContext(Dispatchers.IO) { noBackupFilesDir }

suspend fun Context.getCacheDirSuspend() =
    withContext(Dispatchers.IO) { cacheDir }

suspend fun Context.getFilesDirSuspend() =
    withContext(Dispatchers.IO) { filesDir }
