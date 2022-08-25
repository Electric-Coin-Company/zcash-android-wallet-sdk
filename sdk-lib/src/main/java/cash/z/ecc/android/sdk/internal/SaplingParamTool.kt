package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.exception.TransactionEncoderException
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.ext.deleteRecursivelySuspend
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

// TODO [#666]: https://github.com/zcash/zcash-android-wallet-sdk/issues/666
// TODO [#666]: Download sapling-spend.params and sapling-output.params atomically

// TODO [#665]: https://github.com/zcash/zcash-android-wallet-sdk/issues/665
// TODO [#665]: Recover from corrupted sapling-spend.params and sapling-output.params

// TODO [#664]: https://github.com/zcash/zcash-android-wallet-sdk/issues/664
// TODO [#664]: Check size of download for sapling-spend.params and sapling-output.params

// TODO [#611]: https://github.com/zcash/zcash-android-wallet-sdk/issues/611
// TODO [#611]: Move Params Directory to No Backup Directory

@Suppress("UtilityClassWithPublicConstructor")
class SaplingParamTool {

    companion object {
        /**
         * Checks the given directory for the output and spending params and calls [fetchParams] if
         * they're missing.
         *
         * @param destinationDir the directory where the params should be stored.
         */
        suspend fun ensureParams(destinationDir: String) {
            var hadError = false
            arrayOf(
                ZcashSdk.SPEND_PARAM_FILE_NAME,
                ZcashSdk.OUTPUT_PARAM_FILE_NAME
            ).forEach { paramFileName ->
                if (!File(destinationDir, paramFileName).existsSuspend()) {
                    twig("WARNING: $paramFileName not found at location: $destinationDir")
                    hadError = true
                }
            }
            if (hadError) {
                @Suppress("TooGenericExceptionCaught")
                try {
                    Bush.trunk.twigTask("attempting to download missing params") {
                        fetchParams(destinationDir)
                    }
                } catch (e: Throwable) {
                    twig("failed to fetch params due to: $e")
                    throw TransactionEncoderException.MissingParamsException
                }
            }
        }

        /**
         * Download and store the params into the given directory.
         *
         * @param destinationDir the directory where the params will be stored. It's assumed that we
         * have write access to this directory. Typically, this should be the app's cache directory
         * because it is not harmful if these files are cleared by the user since they are downloaded
         * on-demand.
         */
        suspend fun fetchParams(destinationDir: String) {
            var failureMessage = ""
            arrayOf(
                ZcashSdk.SPEND_PARAM_FILE_NAME,
                ZcashSdk.OUTPUT_PARAM_FILE_NAME
            ).forEach { paramFileName ->
                val url = URL("${ZcashSdk.CLOUD_PARAM_DIR_URL}/$paramFileName")

                val file = File(destinationDir, paramFileName)
                if (file.parentFile?.existsSuspend() == true) {
                    twig("Directory ${file.parentFile?.name} exists!")
                } else {
                    twig("Directory did not exist attempting to make it.")
                    file.parentFile?.mkdirsSuspend()
                }

                withContext(Dispatchers.IO) {
                    Channels.newChannel(url.openStream()).use { readableByteChannel ->
                        FileOutputStream(file).use { fileOutputStream ->
                            fileOutputStream.channel.use { fileChannel ->
                                runCatching {
                                    // transfers bytes from stream channel (position 0 to the end position) into file channel
                                    fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
                                }.onFailure { exception ->
                                    // IllegalArgumentException - If the preconditions on the parameters do not hold
                                    // NonReadableChannelException - If the source channel was not opened for reading
                                    // NonWritableChannelException - If this channel was not opened for writing
                                    // ClosedChannelException - If either this channel or the source channel is closed
                                    // AsynchronousCloseException - If another thread closes either channel while the transfer
                                    // is in progress
                                    // ClosedByInterruptException - If another thread interrupts the current thread while the
                                    // transfer is in progress, thereby closing both channels and setting the current thread's
                                    // interrupt status
                                    // IOException - If some other I/O error occurs
                                    failureMessage += "Error while fetching $paramFileName, caused by $exception\n"
                                    twig(failureMessage)
                                }.onSuccess {
                                    twig("Fetch and write of $paramFileName succeeded.")
                                }
                            }
                        }
                    }
                }
            }
            if (failureMessage.isNotEmpty()) {
                throw TransactionEncoderException.FetchParamsException(failureMessage)
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
                println("Param files${if (!it) "did not" else ""} both exist!")
            }
        }
    }
}
