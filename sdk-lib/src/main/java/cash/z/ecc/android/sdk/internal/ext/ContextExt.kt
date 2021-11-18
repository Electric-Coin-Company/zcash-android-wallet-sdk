package cash.z.ecc.android.sdk.internal.ext

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun Context.getDatabasePathSuspend(fileName: String) =
    withContext(Dispatchers.IO) { getDatabasePath(fileName) }

suspend fun Context.getCacheDirSuspend() =
    withContext(Dispatchers.IO) { cacheDir }
