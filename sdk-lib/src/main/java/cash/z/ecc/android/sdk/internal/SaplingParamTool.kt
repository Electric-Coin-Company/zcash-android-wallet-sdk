package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.exception.TransactionEncoderException
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.ext.deleteRecursivelySuspend
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File

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
            val client = createHttpClient()
            var failureMessage = ""
            arrayOf(
                ZcashSdk.SPEND_PARAM_FILE_NAME,
                ZcashSdk.OUTPUT_PARAM_FILE_NAME
            ).forEach { paramFileName ->
                val url = "${ZcashSdk.CLOUD_PARAM_DIR_URL}/$paramFileName"
                val request = Request.Builder().url(url).build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (response.isSuccessful) {
                    twig("fetch succeeded", -1)
                    val file = File(destinationDir, paramFileName)
                    if (file.parentFile?.existsSuspend() == true) {
                        twig("directory exists!", -1)
                    } else {
                        twig("directory did not exist attempting to make it")
                        file.parentFile?.mkdirsSuspend()
                    }
                    withContext(Dispatchers.IO) {
                        response.body?.let { body ->
                            body.source().use { source ->
                                file.sink().buffer().use { sink ->
                                    twig("writing to $file")
                                    sink.writeAll(source)
                                }
                            }
                        }
                    }
                } else {
                    failureMessage += "Error while fetching $paramFileName : $response\n"
                    twig(failureMessage)
                }

                twig("fetch succeeded, done writing $paramFileName")
            }
            if (failureMessage.isNotEmpty()) throw TransactionEncoderException.FetchParamsException(
                failureMessage
            )
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

        //
        // Helpers
        //
        /**
         * Http client is only used for downloading sapling spend and output params data, which are
         * necessary for the wallet to scan blocks.
         *
         * @return an http client suitable for downloading params data.
         */
        private fun createHttpClient(): OkHttpClient {
            // TODO [#686]: add logging and timeouts
            // TODO [#686]: https://github.com/zcash/zcash-android-wallet-sdk/issues/686
            return OkHttpClient()
        }
    }
}
