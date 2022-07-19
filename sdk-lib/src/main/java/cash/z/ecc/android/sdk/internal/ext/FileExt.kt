@file:Suppress("ktlint:filename")

package cash.z.ecc.android.sdk.internal.ext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal suspend fun File.deleteSuspend() = withContext(Dispatchers.IO) { delete() }

internal suspend fun File.existsSuspend() = withContext(Dispatchers.IO) { exists() }

internal suspend fun File.mkdirsSuspend() = withContext(Dispatchers.IO) { mkdirs() }

internal suspend fun File.renameToSuspend(dest: File) = withContext(Dispatchers.IO) { renameTo(dest) }

suspend fun File.deleteRecursivelySuspend() = withContext(Dispatchers.IO) { deleteRecursively() }
