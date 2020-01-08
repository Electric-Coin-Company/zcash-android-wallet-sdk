package cash.z.wallet.sdk.transaction

import cash.z.wallet.sdk.entity.EncodedTransaction
import cash.z.wallet.sdk.exception.TransactionEncoderException
import cash.z.wallet.sdk.ext.*
import cash.z.wallet.sdk.jni.RustBackend
import cash.z.wallet.sdk.jni.RustBackendWelding
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okio.Okio
import java.io.File

class WalletTransactionEncoder(
    private val rustBackend: RustBackendWelding,
    private val repository: TransactionRepository
) : TransactionEncoder {

    /**
     * Creates a transaction, throwing an exception whenever things are missing. When the provided wallet implementation
     * doesn't throw an exception, we wrap the issue into a descriptive exception ourselves (rather than using
     * double-bangs for things).
     */
    override suspend fun createTransaction(
        spendingKey: String,
        zatoshi: Long,
        toAddress: String,
        memo: ByteArray?,
        fromAccountIndex: Int
    ): EncodedTransaction = withContext(IO) {
        val transactionId = createSpend(spendingKey, zatoshi, toAddress, memo)
        repository.findEncodedTransactionById(transactionId)
            ?: throw TransactionEncoderException.TransactionNotFoundException(transactionId)
    }

    /**
     * Utility function to help with validation. This is not called during [createTransaction]
     * because this class asserts that all validation is done externally by the UI, for now.
     */
    override suspend fun isValidShieldedAddress(address: String): Boolean = withContext(IO) {
        rustBackend.isValidShieldedAddr(address)
    }

    /**
     * Utility function to help with validation. This is not called during [createTransaction]
     * because this class asserts that all validation is done externally by the UI, for now.
     */
    override suspend fun isValidTransparentAddress(address: String): Boolean = withContext(IO) {
        rustBackend.isValidTransparentAddr(address)
    }

    /**
     * Does the proofs and processing required to create a transaction to spend funds and inserts
     * the result in the database. On average, this call takes over 10 seconds.
     *
     * @param value the zatoshi value to send
     * @param toAddress the destination address
     * @param memo the memo, which is not augmented in any way
     *
     * @return the row id in the transactions table that contains the spend transaction
     * or -1 if it failed
     */
    private suspend fun createSpend(
        spendingKey: String,
        value: Long,
        toAddress: String,
        memo: ByteArray? = byteArrayOf(),
        fromAccountIndex: Int = 0
    ): Long = withContext(IO) {
        twigTask("creating transaction to spend $value zatoshi to" +
                " ${toAddress.masked()} with memo $memo") {
            try {
                ensureParams((rustBackend as RustBackend).paramDestinationDir)
                twig("params exist! attempting to send...")
                rustBackend.createToAddress(
                    fromAccountIndex,
                    spendingKey,
                    toAddress,
                    value,
                    memo
                )
            } catch (t: Throwable) {
                twig("${t.message}")
                throw t
            }
        }.also { result ->
            twig("result of sendToAddress: $result")
        }
    }

    /**
     * Checks the given directory for the output and spending params and calls [fetchParams] if
     * they're missing.
     *
     * @param destinationDir the directory where the params should be stored.
     */
    private suspend fun ensureParams(destinationDir: String) {
        var hadError = false
        arrayOf(
            ZcashSdk.SPEND_PARAM_FILE_NAME,
            ZcashSdk.OUTPUT_PARAM_FILE_NAME
        ).forEach { paramFileName ->
            if (!File(destinationDir, paramFileName).exists()) {
                twig("ERROR: $paramFileName not found at location: $destinationDir")
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
    suspend fun fetchParams(destinationDir: String) = withContext(IO) {
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
                twig("fetch succeeded")
                val file = File(destinationDir, paramFileName)
                if(file.parentFile.exists()) {
                    twig("directory exists!")
                } else {
                    twig("directory did not exist attempting to make it")
                    file.parentFile.mkdirs()
                }
                Okio.buffer(Okio.sink(file)).use {
                    twig("writing to $file")
                    it.writeAll(response.body().source())
                }
                twig("fetch succeeded, done writing $paramFileName")
            } else {
                failureMessage += "Error while fetching $paramFileName : $response\n"
                twig(failureMessage)
            }
        }
        if (failureMessage.isNotEmpty()) throw TransactionEncoderException.FetchParamsException(failureMessage)
    }


    //
    // Helpers
    //

    /**
     * Http client is only used for downloading sapling spend and output params data, which are
     * necessary for the wallet to scan blocks.
     */
    private fun createHttpClient(): OkHttpClient {
        //TODO: add logging and timeouts
        return OkHttpClient()
    }

}
