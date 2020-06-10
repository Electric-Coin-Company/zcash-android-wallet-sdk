package cash.z.ecc.android.sdk.transaction

import cash.z.ecc.android.sdk.db.entity.EncodedTransaction
import cash.z.ecc.android.sdk.exception.TransactionEncoderException
import cash.z.ecc.android.sdk.ext.*
import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.jni.RustBackendWelding
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okio.Okio
import java.io.File

/**
 * Class responsible for encoding a transaction in a consistent way. This bridges the gap by
 * behaving like a stateless API so that callers can request [createTransaction] and receive a
 * result, even though there are intermediate database interactions.
 *
 * @property rustBackend the instance of RustBackendWelding to use for creating and validating.
 * @property repository the repository that stores information about the transactions being created
 * such as the raw bytes and raw txId.
 */
class WalletTransactionEncoder(
    private val rustBackend: RustBackendWelding,
    private val repository: TransactionRepository
) : TransactionEncoder {

    /**
     * Creates a transaction, throwing an exception whenever things are missing. When the provided
     * wallet implementation doesn't throw an exception, we wrap the issue into a descriptive
     * exception ourselves (rather than using double-bangs for things).
     *
     * @param spendingKey the key associated with the notes that will be spent.
     * @param zatoshi the amount of zatoshi to send.
     * @param toAddress the recipient's address.
     * @param memo the optional memo to include as part of the transaction.
     * @param fromAccountIndex the optional account id to use. By default, the 1st account is used.
     *
     * @return the successfully encoded transaction or an exception
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
     *
     * @param address the address to validate
     *
     * @return true when the given address is a valid z-addr
     */
    override suspend fun isValidShieldedAddress(address: String): Boolean = withContext(IO) {
        rustBackend.isValidShieldedAddr(address)
    }

    /**
     * Utility function to help with validation. This is not called during [createTransaction]
     * because this class asserts that all validation is done externally by the UI, for now.
     *
     * @param address the address to validate
     *
     * @return true when the given address is a valid t-addr
     */
    override suspend fun isValidTransparentAddress(address: String): Boolean = withContext(IO) {
        rustBackend.isValidTransparentAddr(address)
    }

    override suspend fun getConsensusBranchId(): Long {
        val height = repository.lastScannedHeight()
        if (height < ZcashSdk.SAPLING_ACTIVATION_HEIGHT)
            throw TransactionEncoderException.IncompleteScanException(height)
        return rustBackend.getBranchIdForHeight(height)
    }

    /**
     * Does the proofs and processing required to create a transaction to spend funds and inserts
     * the result in the database. On average, this call takes over 10 seconds.
     *
     * @param spendingKey the key associated with the notes that will be spent.
     * @param zatoshi the amount of zatoshi to send.
     * @param toAddress the recipient's address.
     * @param memo the optional memo to include as part of the transaction.
     * @param fromAccountIndex the optional account id to use. By default, the 1st account is used.
     *
     * @return the row id in the transactions table that contains the spend transaction or -1 if it
     * failed.
     */
    private suspend fun createSpend(
        spendingKey: String,
        zatoshi: Long,
        toAddress: String,
        memo: ByteArray? = byteArrayOf(),
        fromAccountIndex: Int = 0
    ): Long = withContext(IO) {
        twigTask("creating transaction to spend $zatoshi zatoshi to" +
                " ${toAddress.masked()} with memo $memo") {
            try {
                val branchId = getConsensusBranchId()
                ensureParams((rustBackend as RustBackend).pathParamsDir)
                twig("params exist! attempting to send with consensus branchId $branchId...")
                rustBackend.createToAddress(
                    branchId,
                    fromAccountIndex,
                    spendingKey,
                    toAddress,
                    zatoshi,
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
     *
     * @return an http client suitable for downloading params data.
     */
    private fun createHttpClient(): OkHttpClient {
        //TODO: add logging and timeouts
        return OkHttpClient()
    }

}
