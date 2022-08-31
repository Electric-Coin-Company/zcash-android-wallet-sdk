@file:Suppress("ktlint:filename")

package cash.z.ecc.android.sdk.internal.ext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

internal suspend fun File.deleteSuspend() = withContext(Dispatchers.IO) { delete() }

internal suspend fun File.existsSuspend() = withContext(Dispatchers.IO) { exists() }

internal suspend fun File.mkdirsSuspend() = withContext(Dispatchers.IO) { mkdirs() }

internal suspend fun File.canWriteSuspend() = withContext(Dispatchers.IO) { canWrite() }

internal suspend fun File.renameToSuspend(dest: File) = withContext(Dispatchers.IO) { renameTo(dest) }

suspend fun File.deleteRecursivelySuspend() = withContext(Dispatchers.IO) { deleteRecursively() }

/**
 * Encrypts File to SHA1 format. This method is not recommended on huge files. It has an internal limitation of 2 GB
 * byte array size.
 *
 * @return String SHA1 encryption of the input file
 *
 * @throws OutOfMemoryError if the file is too big to fit in memory.
 */
@Throws(OutOfMemoryError::class)
fun File.getSha1Hash(): String {
    return MessageDigest
        .getInstance("SHA-1")
        .digest(readBytes())
        .joinToString(
            separator = "",
            transform = { "%02x".format(it) }
        )
}
