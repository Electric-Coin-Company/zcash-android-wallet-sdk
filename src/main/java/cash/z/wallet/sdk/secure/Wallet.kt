package cash.z.wallet.sdk.secure

import cash.z.wallet.sdk.data.*
import cash.z.wallet.sdk.exception.WalletException
import cash.z.wallet.sdk.ext.masked
import cash.z.wallet.sdk.jni.JniConverter
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okio.Okio
import java.io.File
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty


/**
 * Wrapper for the converter. This class basically represents all the Rust-wallet capabilities and the supporting data
 * required to exercise those abilities.
 */
class Wallet(
    private val converter: JniConverter,
    private val dbDataPath: String,
    private val paramDestinationDir: String,
    /** indexes of accounts ids. In the reference wallet, we only work with account 0 */
    private val accountIds: Array<Int> = arrayOf(0),
    private val seedProvider: ReadOnlyProperty<Any?, ByteArray>,
    spendingKeyProvider: ReadWriteProperty<Any?, String>
) {
    var spendingKeyStore by spendingKeyProvider

    init {
        // initialize data db for this wallet and its accounts
        // initialize extended viewing keys for this wallet's seed and store them in the dataDb
        // initialize spending keys

        // call converter.initializeForSeed(seed, n) where n is the number of accounts
        // get back an array of spending keys for each account. store them super securely
    }

    fun initialize(firstRunStartHeight: Int = 280000): Int {
        twig("Initializing wallet for first run")
        converter.initDataDb(dbDataPath)
        //TODO: pass this into the synchronizer and leverage it here
        converter.initBlocksTable(dbDataPath, 421720, 1550762014, "015495a30aef9e18b9c774df6a9fcd583748c8bba1a6348e70f59bc9f0c2bc673b000f00000000018054b75173b577dc36f2c80dfc41f83d6716557597f74ec54436df32d4466d57000120f1825067a52ca973b07431199d5866a0d46ef231d08aa2f544665936d5b4520168d782e3d028131f59e9296c75de5a101898c5e53108e45baa223c608d6c3d3d01fb0a8d465b57c15d793c742df9470b116ddf06bd30d42123fdb7becef1fd63640001a86b141bdb55fd5f5b2e880ea4e07caf2bbf1ac7b52a9f504977913068a917270001dd960b6c11b157d1626f0768ec099af9385aea3f31c91111a8c5b899ffb99e6b0192acd61b1853311b0bf166057ca433e231c93ab5988844a09a91c113ebc58e18019fbfd76ad6d98cafa0174391546e7022afe62e870e20e16d57c4c419a5c2bb69")

        // securely store the spendingkey by leveraging the utilities provided during construction
        val seed by seedProvider
        val accountSpendingKeys = converter.initAccountsTable(dbDataPath, seed, 1)
        spendingKeyStore = accountSpendingKeys[0]

//        converter.initBlocksTable(dbData, height, time, saplingTree)
        // TODO: init blocks table with sapling tree. probably read a table row in and then write it out to disk in a way where we can deserialize easily
        // TODO: then use that to determine firstRunStartHeight

//        val firstRunStartHeight = 405410
        return 421720
    }

    fun getAddress(accountId: Int = accountIds[0]): String {
        return converter.getAddress(dbDataPath, accountId)
    }

    fun getBalance(accountId: Int = accountIds[0]) {
        // TODO: modify request to factor in account Ids
        converter.getBalance(dbDataPath, accountId)
    }

    /**
     * Does the proofs and processing required to create a raw transaction and inserts the result in the database. On
     * average, this call takes over 10 seconds.
     *
     * @param value the zatoshi value to send
     * @param toAddress the destination address
     * @param memo the memo, which is not augmented in any way
     *
     * @return the row id in the transactions table that contains the raw transaction or -1 if it failed
     */
    suspend fun createRawSendTransaction(value: Long, toAddress: String, memo: String = "", fromAccountId: Int = accountIds[0]): Long =
        withContext(IO) {
            var result = -1L
            Bush.trunk.twigTask("creating raw transaction to send $value zatoshi to ${toAddress.masked()}") {
                result = runCatching {
                    ensureParams(paramDestinationDir)
                    twig("params exist at $paramDestinationDir! attempting to send...")
                    converter.sendToAddress(
                        dbDataPath,
                        fromAccountId,
                        spendingKeyStore,
                        toAddress,
                        value,
                        memo,
                        // using names here so it's easier to avoid transposing them, if the function signature changes
                        spendParams = SPEND_PARAM_FILE_NAME.toPath(),
                        outputParams = OUTPUT_PARAM_FILE_NAME.toPath()
                    )
                }.getOrDefault(result)
            }
            twig("result of sendToAddress: $result")
            result
        }

    suspend fun fetchParams(destinationDir: String) = withContext(IO) {
        val client = createHttpClient()
        var failureMessage = ""
        arrayOf(SPEND_PARAM_FILE_NAME, OUTPUT_PARAM_FILE_NAME).forEach { paramFileName ->
            val url = "$CLOUD_PARAM_DIR_URL/$paramFileName"
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
        if (failureMessage.isNotEmpty()) throw WalletException.FetchParamsException(failureMessage)
    }

    private suspend fun ensureParams(destinationDir: String) {
        var hadError = false
        arrayOf(SPEND_PARAM_FILE_NAME, OUTPUT_PARAM_FILE_NAME).forEach { paramFileName ->
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
                throw WalletException.MissingParamsException
            }
        }
    }


    //
    // Helpers
    //

    private fun createHttpClient(): OkHttpClient {
        //TODO: add logging and timeouts
        return OkHttpClient()
    }


    private fun String.toPath(): String = "$paramDestinationDir/$this"

    companion object {
        /**
         * The Url that is used by default in zcashd.
         * We'll want to make this externally configurable, rather than baking it into the SDK but this will do for now,
         * since we're using a cloudfront URL that already redirects.
         */
        const val CLOUD_PARAM_DIR_URL = "https://z.cash/downloads/"
        const val SPEND_PARAM_FILE_NAME = "sapling-spend.params"
        const val OUTPUT_PARAM_FILE_NAME = "sapling-output.params"
    }
}
