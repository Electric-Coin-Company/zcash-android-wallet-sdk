package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.exception.TransactionEncoderException
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.ext.deleteRecursivelySuspend
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.nio.channels.Channels

// TODO [#666]: https://github.com/zcash/zcash-android-wallet-sdk/issues/666
// TODO [#666]: Download sapling-spend.params and sapling-output.params atomically

// TODO [#665]: https://github.com/zcash/zcash-android-wallet-sdk/issues/665
// TODO [#665]: Recover from corrupted sapling-spend.params and sapling-output.params

// TODO [#611]: https://github.com/zcash/zcash-android-wallet-sdk/issues/611
// TODO [#611]: Move Params Directory to No Backup Directory

object SaplingParamTool {
    /**
     * Maximum file size for the sapling spend params - 50MB
     */
    internal const val SPEND_PARAM_FILE_MAX_BYTES_SIZE = 50L * 1024L * 1024L

    /**
     * Maximum file size for the sapling spend params - 5MB
     */
    internal const val OUTPUT_PARAM_FILE_MAX_BYTES_SIZE = 5L * 1024L * 1024L

    /**
     * Checks the given directory for the output and spending params and calls [fetchParams] for those, which are
     * missing.
     *
     * @param destinationDir the directory where the params should be stored.
     *
     * @throws TransactionEncoderException.MissingParamsException in case of failure while checking sapling params
     * files
     */
    @Throws(TransactionEncoderException.MissingParamsException::class)
    suspend fun ensureParams(destinationDir: String) {
        arrayOf(
            SaplingFileParameters(
                destinationDir,
                ZcashSdk.SPEND_PARAM_FILE_NAME,
                SPEND_PARAM_FILE_MAX_BYTES_SIZE
            ),
            SaplingFileParameters(
                destinationDir,
                ZcashSdk.OUTPUT_PARAM_FILE_NAME,
                OUTPUT_PARAM_FILE_MAX_BYTES_SIZE
            )
        ).filter {
            !File(it.destinationDirectoryPath, it.fileName).existsSuspend()
        }.forEach {
            try {
                twig("Attempting to download missing params: ${it.fileName}.")
                fetchParams(it)
            } catch (e: TransactionEncoderException.FetchParamsException) {
                twig("Failed to fetch params ${it.fileName} due to: $e")
                throw TransactionEncoderException.MissingParamsException
            }
        }

        if (!validate(destinationDir)) {
            twig("Fetching sapling params files failed.")
            throw TransactionEncoderException.MissingParamsException
        }
    }

    /**
     * Download and store the params file into the given directory. It also checks the file size to eliminate the
     * risk of downloading potentially large file from a malicious server.
     *
     * @param paramsToFetch parameters wrapper class, which holds information about it
     *
     * @throws TransactionEncoderException.FetchParamsException if any error while downloading the params file occurs
     */
    @Throws(TransactionEncoderException.FetchParamsException::class)
    internal suspend fun fetchParams(paramsToFetch: SaplingFileParameters) {
        val url = URL("${ZcashSdk.CLOUD_PARAM_DIR_URL}/${paramsToFetch.fileName}")

        val file = File(paramsToFetch.destinationDirectoryPath, paramsToFetch.fileName)
        if (file.parentFile?.existsSuspend() == true) {
            twig("Directory ${file.parentFile?.name} exists!")
        } else {
            twig("Directory did not exist attempting to make it.")
            file.parentFile?.mkdirsSuspend()
        }

        withContext(Dispatchers.IO) {
            runCatching {
                Channels.newChannel(url.openStream()).use { readableByteChannel ->
                    file.outputStream().use { fileOutputStream ->
                        fileOutputStream.channel.use { fileChannel ->
                            // Transfers bytes from stream to file from position 0 to end position or to max
                            // file size limit. This eliminates the risk of downloading potentially large files
                            // from a malicious server. We need to make a check of the file hash then.
                            fileChannel.transferFrom(readableByteChannel, 0, paramsToFetch.fileMaxSizeBytes)
                        }
                    }
                }
            }.onFailure { exception ->
                // IllegalArgumentException - If the preconditions on the parameters do not hold
                // NonReadableChannelException - If the source channel was not opened for reading
                // NonWritableChannelException - If this channel was not opened for writing
                // ClosedChannelException - If either this channel or the source channel is closed
                // AsynchronousCloseException - If another thread closes either channel while the transfer is
                // in progress
                // ClosedByInterruptException - If another thread interrupts the current thread while the
                // transfer is in progress, thereby closing both channels and setting the current thread's
                // interrupt status
                // IOException - If some other I/O error occurs
                "Error while fetching ${paramsToFetch.fileName}, caused by $exception".also {
                    twig(it)
                    throw TransactionEncoderException.FetchParamsException(it)
                }
            }.onSuccess {
                twig("Fetch and write of ${paramsToFetch.fileName} succeeded.")
            }
        }
    }

    suspend fun clear(destinationDir: String) {
        if (validate(destinationDir)) {
            arrayOf(
                ZcashSdk.SPEND_PARAM_FILE_NAME,
                ZcashSdk.OUTPUT_PARAM_FILE_NAME
            ).forEach { paramFileName ->
                val file = File(destinationDir, paramFileName)
                if (file.deleteRecursivelySuspend()) {
                    twig("Files deleted successfully")
                } else {
                    twig("Error: Files not able to be deleted!")
                }
            }
        }
    }

    suspend fun validate(destinationDir: String): Boolean {
        return arrayOf(
            ZcashSdk.SPEND_PARAM_FILE_NAME,
            ZcashSdk.OUTPUT_PARAM_FILE_NAME
        ).all { paramFileName ->
            File(destinationDir, paramFileName).existsSuspend()
        }.also {
            println("Param files ${if (!it) "did not" else ""} both exist!")
        }
    }
}

/**
 * Sapling file parameters class to hold each sapling file attributes.
 */
internal data class SaplingFileParameters(
    val destinationDirectoryPath: String,
    val fileName: String,
    val fileMaxSizeBytes: Long
)
