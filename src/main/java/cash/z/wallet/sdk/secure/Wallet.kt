package cash.z.wallet.sdk.secure

import android.content.Context
import cash.z.wallet.sdk.data.Bush
import cash.z.wallet.sdk.data.CompactBlockProcessor.Companion.SAPLING_ACTIVATION_HEIGHT
import cash.z.wallet.sdk.data.twig
import cash.z.wallet.sdk.data.twigTask
import cash.z.wallet.sdk.exception.RustLayerException
import cash.z.wallet.sdk.exception.WalletException
import cash.z.wallet.sdk.ext.masked
import cash.z.wallet.sdk.jni.JniConverter
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.withContext
import okio.Okio
import java.io.File
import java.io.InputStreamReader
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty


/**
 * Wrapper for the converter. This class basically represents all the Rust-wallet capabilities and the supporting data
 * required to exercise those abilities.
 */
class Wallet(
    private val birthday: WalletBirthday,
    private val converter: JniConverter,
    private val dataDbPath: String,
    private val paramDestinationDir: String,
    /** indexes of accounts ids. In the reference wallet, we only work with account 0 */
    private val accountIds: Array<Int> = arrayOf(0),
    private val seedProvider: ReadOnlyProperty<Any?, ByteArray>,
    spendingKeyProvider: ReadWriteProperty<Any?, String>
) {
    constructor(
        context: Context,
        converter: JniConverter,
        dataDbPath: String,
        paramDestinationDir: String,
        accountIds: Array<Int> = arrayOf(0),
        seedProvider: ReadOnlyProperty<Any?, ByteArray>,
        spendingKeyProvider: ReadWriteProperty<Any?, String>
    ) : this(
        birthday = loadBirthdayFromAssets(context),
        converter = converter,
        dataDbPath = dataDbPath,
        paramDestinationDir = paramDestinationDir,
        accountIds = accountIds,
        seedProvider = seedProvider,
        spendingKeyProvider = spendingKeyProvider
    )

    var spendingKeyStore by spendingKeyProvider

    private val balanceChannel = ConflatedBroadcastChannel<WalletBalance>()

    fun initialize(
        firstRunStartHeight: Int = SAPLING_ACTIVATION_HEIGHT
    ): Int {
        twig("Initializing wallet for first run")
        converter.initDataDb(dataDbPath)
        twig("seeding the database with sapling tree at height ${birthday.height}")
        converter.initBlocksTable(dataDbPath, birthday.height, birthday.time, birthday.tree)

        // securely store the spendingkey by leveraging the utilities provided during construction
        val seed by seedProvider
        val accountSpendingKeys = converter.initAccountsTable(dataDbPath, seed, 1)
        spendingKeyStore = accountSpendingKeys[0]

//        converter.initBlocksTable(dbData, height, time, saplingTree)
        // TODO: init blocks table with sapling tree. probably read a table row in and then write it out to disk in a way where we can deserialize easily
        // TODO: then use that to determine firstRunStartHeight

        return Math.max(firstRunStartHeight, birthday.height)
    }

    fun getAddress(accountId: Int = accountIds[0]): String {
        return converter.getAddress(dataDbPath, accountId)
    }

    fun balance(): ReceiveChannel<WalletBalance> {
        return balanceChannel.openSubscription()
    }

    suspend fun sendBalanceInfo(accountId: Int = accountIds[0]) = withContext(IO) {
        twigTask("checking balance info") {
            try {
                val balanceTotal = converter.getBalance(dataDbPath, accountId)
                twig("found total balance of: $balanceTotal")
                val balanceAvailable = converter.getVerifiedBalance(dataDbPath, accountId)
                twig("found available balance of: $balanceAvailable")
                balanceChannel.send(WalletBalance(balanceTotal, balanceAvailable))
            } catch (t: Throwable) {
                twig("failed to get balance due to $t")
                throw RustLayerException.BalanceException(t)
            }
        }
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
            twigTask("creating raw transaction to send $value zatoshi to ${toAddress.masked()}") {
                result = runCatching {
                    ensureParams(paramDestinationDir)
                    twig("params exist at $paramDestinationDir! attempting to send...")
                    converter.sendToAddress(
                        dataDbPath,
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

        const val BIRTHDAY_DIRECTORY = "zcash/saplingtree"

        /**
         * Load the given birthday file from the assets of the given context. When no height is specified, we default to
         * the file with the greatest name.
         *
         * @param context the context from which to load assets.
         * @param birthdayHeight the height file to look for among the file names.
         *
         * @return a WalletBirthday that reflects the contents of the file or an exception when parsing fails.
         */
        fun loadBirthdayFromAssets(context: Context, birthdayHeight: Int? = null): WalletBirthday {
            val treeFiles = context.assets.list(Wallet.BIRTHDAY_DIRECTORY).apply { sortDescending() }
            if (treeFiles.isEmpty()) throw WalletException.MissingBirthdayFilesException(BIRTHDAY_DIRECTORY)
            try {
                val file = treeFiles.first {
                    if (birthdayHeight == null) true
                    else it.contains(birthdayHeight.toString())
                }
                val reader =
                    JsonReader(InputStreamReader(context.assets.open("$BIRTHDAY_DIRECTORY/$file")))
                return Gson().fromJson(reader, WalletBirthday::class.java)
            } catch (t: Throwable) {
                throw WalletException.MalformattedBirthdayFilesException(BIRTHDAY_DIRECTORY, treeFiles[0])
            }
        }

    }

    data class WalletBirthday(
        val height: Int = -1,
        val time: Long = -1,
        val tree: String = ""
    )

    data class WalletBalance(
        val total: Long = -1,
        val available: Long = -1
    )

}
