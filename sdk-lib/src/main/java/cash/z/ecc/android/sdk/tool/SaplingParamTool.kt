package cash.z.ecc.android.sdk.tool

import cash.z.ecc.android.sdk.exception.TransactionEncoderException
import cash.z.ecc.android.sdk.ext.Bush
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.ext.twigTask
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Okio
import java.io.File

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
                if (!File(destinationDir, paramFileName).exists()) {
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
        suspend fun fetchParams(destinationDir: String) = withContext(Dispatchers.IO) {
            val client = createHttpClient()
            var failureMessage = ""
            arrayOf(
                ZcashSdk.SPEND_PARAM_FILE_NAME,
                ZcashSdk.OUTPUT_PARAM_FILE_NAME
            ).forEach { paramFileName ->
                val url = "${ZcashSdk.CLOUD_PARAM_DIR_URL}/$paramFileName"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    twig("fetch succeeded", -1)
                    val file = File(destinationDir, paramFileName)
                    if (file.parentFile.exists()) {
                        twig("directory exists!", -1)
                    } else {
                        twig("directory did not exist attempting to make it")
                        file.parentFile.mkdirs()
                    }
                    Okio.buffer(Okio.sink(file)).use {
                        twig("writing to $file")
                        it.writeAll(response.body().source())
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

        fun clear(destinationDir: String) {
            if (validate(destinationDir)) {
                arrayOf(
                    ZcashSdk.SPEND_PARAM_FILE_NAME,
                    ZcashSdk.OUTPUT_PARAM_FILE_NAME
                ).forEach { paramFileName ->
                    val file = File(destinationDir, paramFileName)
                    if (file.deleteRecursively()) {
                        twig("Files deleted successfully")
                    } else {
                        twig("Error: Files not able to be deleted!")
                    }
                }
            }
        }

        fun validate(destinationDir: String): Boolean {
            return arrayOf(
                ZcashSdk.SPEND_PARAM_FILE_NAME,
                ZcashSdk.OUTPUT_PARAM_FILE_NAME
            ).all { paramFileName ->
                File(destinationDir, paramFileName).exists()
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
            // TODO: add logging and timeouts
            return OkHttpClient()
        }
    }
}
