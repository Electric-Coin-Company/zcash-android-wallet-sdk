package cash.z.wallet.sdk.secure

import android.content.Context
import cash.z.wallet.sdk.data.Bush
import cash.z.wallet.sdk.data.twig
import cash.z.wallet.sdk.data.twigTask
import cash.z.wallet.sdk.exception.RustLayerException
import cash.z.wallet.sdk.exception.WalletException
import cash.z.wallet.sdk.exception.WalletException.*
import cash.z.wallet.sdk.ext.ZcashSdk.DB_CACHE_NAME
import cash.z.wallet.sdk.ext.ZcashSdk.DB_DATA_NAME
import cash.z.wallet.sdk.ext.ZcashSdk.OUTPUT_PARAM_FILE_NAME
import cash.z.wallet.sdk.ext.ZcashSdk.SAPLING_ACTIVATION_HEIGHT
import cash.z.wallet.sdk.ext.ZcashSdk.SPEND_PARAM_FILE_NAME
import cash.z.wallet.sdk.ext.masked
import cash.z.wallet.sdk.jni.RustBackend
import cash.z.wallet.sdk.jni.RustBackendWelding
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okio.Okio
import java.io.File
import java.io.InputStreamReader


/**
 * Wrapper for all the Rust backend functionality that does not involve processing blocks. This
 * class initializes the Rust backend and the supporting data required to exercise those abilities.
 * The [cash.z.wallet.sdk.block.CompactBlockProcessor] handles all the remaining Rust backend
 * functionality, related to processing blocks.
 */
class Wallet {

    lateinit var rustBackend: RustBackendWelding
    var lowerBoundHeight: Int = SAPLING_ACTIVATION_HEIGHT

    fun clear() {
        if (::rustBackend.isInitialized) {
            (rustBackend as RustBackend).clear()
        } else {
            twig("WARNING: attempted to clear an uninitialized wallet. No action was taken since " +
                    "the database paths have not yet been set.")
        }
    }

    /**
     * Initialize the wallet with the given seed and return the related private keys for each
     * account specified or null if the wallet was previously initialized and block data exists on
     * disk. When this method returns null, that signals that the wallet will need to retrieve the
     * private keys from its own secure storage. In other words, the private keys are only given out
     * once for each set of database files. Subsequent calls to [initialize] will only load the Rust
     * library and return null.
     *
     * 'compactBlockCache.db' and 'transactionData.db' files are created by this function (if they
     * do not already exist). These files can be given a prefix for scenarios where multiple wallets
     * operate in one app--for instance, when sweeping funds from another wallet seed.
     *
     * @param appContext the application context.
     * @param seed the seed to use for initializing this wallet.
     * @param birthdayHeight the height corresponding to when the wallet seed was created. If null,
     * this signals that the wallet is being born.
     * @param numberOfAccounts the number of accounts to create from this seed.
     * @param dbFileNamePrefix the optional prefix to add to the names of the database files.
     */
    fun initialize(
        appContext: Context,
        seed: ByteArray,
        birthdayHeight: Int? = null,
        numberOfAccounts: Int = 1,
        dbFileNamePrefix: String = ""
    ): Array<String>? {
        rustBackend = RustBackend.create(
            appContext,
            "${dbFileNamePrefix}$DB_CACHE_NAME",
            "${dbFileNamePrefix}$DB_DATA_NAME"
        )

        try {
            // only creates tables, if they don't exist
            rustBackend.initDataDb()
            twig("Initialized wallet for first run")
        } catch (e: Throwable) {
            throw WalletException.FalseStart(e)
        }

        try {
            val birthday = loadBirthdayFromAssets(appContext, birthdayHeight)
            lowerBoundHeight = birthday.height
            rustBackend.initBlocksTable(
                birthday.height,
                birthday.hash,
                birthday.time,
                birthday.tree
            )
            twig("seeded the database with sapling tree at height ${birthday.height} (expected $birthdayHeight)")
        } catch (t: Throwable) {
            if (t.message?.contains("is not empty") == true) {
                return null
            } else {
                throw WalletException.FalseStart(t)
            }
        }

        try {
            return rustBackend.initAccountsTable(seed, numberOfAccounts).also {
                twig("Initialized the accounts table with $numberOfAccounts account(s)")
            }
        } catch (e: Throwable) {
            throw WalletException.FalseStart(e)
        }
    }

    /**
     * Gets the address for this wallet, defaulting to the first account.
     */
    fun getAddress(accountIndex: Int = 0): String {
        return rustBackend.getAddress(accountIndex)
    }

    /**
     * Return a quick snapshot of the available balance. In most cases, the stream of balances
     * provided by [balances] should be used instead of this funciton.
     *
     * @param accountIndex the account to check for balance info. Defaults to zero.
     */
    fun availableBalanceSnapshot(accountIndex: Int = 0): Long {
        return rustBackend.getVerifiedBalance(accountIndex)
    }

    /**
     * Calculates the latest balance info and emits it into the balance channel. Defaults to the
     * first account.
     *
     * @param accountIndex the account to check for balance info.
     */
    suspend fun getBalanceInfo(accountIndex: Int = 0): WalletBalance = withContext(IO) {
        twigTask("checking balance info") {
            try {
                val balanceTotal = rustBackend.getBalance(accountIndex)
                twig("found total balance of: $balanceTotal")
                val balanceAvailable = rustBackend.getVerifiedBalance(accountIndex)
                twig("found available balance of: $balanceAvailable")
                WalletBalance(balanceTotal, balanceAvailable)
            } catch (t: Throwable) {
                twig("failed to get balance due to $t")
                throw RustLayerException.BalanceException(t)
            }
        }
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
    suspend fun createSpend(
        spendingKey: String,
        value: Long,
        toAddress: String,
        memo: String = "",
        fromAccountIndex: Int = 0
    ): Long = withContext(IO) {
        twigTask(
            "creating transaction to spend $value zatoshi to" +
                    " ${toAddress.masked()} with memo $memo"
        ) {
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

    /**
     * Checks the given directory for the output and spending params and calls [fetchParams] if
     * they're missing.
     *
     * @param destinationDir the directory where the params should be stored.
     */
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

    /**
     * Http client is only used for downloading sapling spend and output params data, which are
     * necessary for the wallet to scan blocks.
     */
    private fun createHttpClient(): OkHttpClient {
        //TODO: add logging and timeouts
        return OkHttpClient()
    }

    companion object {
        /**
         * The Url that is used by default in zcashd.
         * We'll want to make this externally configurable, rather than baking it into the SDK but
         * this will do for now, since we're using a cloudfront URL that already redirects.
         */
        const val CLOUD_PARAM_DIR_URL = "https://z.cash/downloads/"

        /**
         * Directory within the assets folder where birthday data
         * (i.e. sapling trees for a given height) can be found.
         */
        const val BIRTHDAY_DIRECTORY = "zcash/saplingtree"

        /**
         * Load the given birthday file from the assets of the given context. When no height is
         * specified, we default to the file with the greatest name.
         *
         * @param context the context from which to load assets.
         * @param birthdayHeight the height file to look for among the file names.
         *
         * @return a WalletBirthday that reflects the contents of the file or an exception when
         * parsing fails.
         */
        fun loadBirthdayFromAssets(context: Context, birthdayHeight: Int? = null): WalletBirthday {
            val treeFiles =
                context.assets.list(BIRTHDAY_DIRECTORY)?.apply { sortDescending() }
            if (treeFiles.isNullOrEmpty()) throw MissingBirthdayFilesException(BIRTHDAY_DIRECTORY)
            val file: String
            try {
                file = treeFiles.first() {
                    if (birthdayHeight == null) true
                    else it.contains(birthdayHeight.toString())
                }
            } catch (t: Throwable) {
                throw BirthdayNotFoundException(BIRTHDAY_DIRECTORY, birthdayHeight)
            }
            try {
                val reader = JsonReader(
                    InputStreamReader(context.assets.open("$BIRTHDAY_DIRECTORY/$file"))
                )
                return Gson().fromJson(reader, WalletBirthday::class.java)
            } catch (t: Throwable) {
                throw MalformattedBirthdayFilesException(BIRTHDAY_DIRECTORY, treeFiles[0])
            }
        }

    }

    /**
     * Represents the wallet's birthday which can be thought of as a checkpoint at the earliest
     * moment in history where transactions related to this wallet could exist. Ideally, this would
     * correspond to the latest block height at the time the wallet key was created. Worst case, the
     * height of Sapling activation could be used (280000).
     *
     * Knowing a wallet's birthday can significantly reduce the amount of data that it needs to
     * download because none of the data before that height needs to be scanned for transactions.
     * However, we do need the Sapling tree data in order to construct valid transactions from that
     * point forward. This birthday contains that tree data, allowing us to avoid downloading all
     * the compact blocks required in order to generate it.
     *
     * Currently, the data for this is generated by running `cargo run --release --features=updater`
     * with the SDK and saving the resulting JSON to the `src/main/assets/zcash` folder. That script
     * simply builds a Sapling tree from the start of Sapling activation up to the latest block
     * height. In the future, this data could be exposed as a service on the lightwalletd server
     * because every zcashd node already maintains the sapling tree for each block. For now, we just
     * include the latest checkpoint in each SDK release.
     *
     * New wallets can ignore any blocks created before their birthday.
     *
     * @param height the height at the time the wallet was born
     * @param hash the block hash corresponding to the given height
     * @param time the time the wallet was born, in seconds
     * @param tree the sapling tree corresponding to the given height. This takes around 15 minutes
     * of processing to generate from scratch because all blocks since activation need to be
     * considered. So when it is calculated in advance it can save the user a lot of time.
     */
    data class WalletBirthday(
        val height: Int = -1,
        val hash: String = "",
        val time: Long = -1,
        val tree: String = ""
    )

    /**
     * Data structure to hold the total and available balance of the wallet. This is what is
     * received on the balance channel.
     *
     * @param total the total balance, ignoring funds that cannot be used.
     * @param available the amount of funds that are available for use. Typical reasons that funds
     * may be unavailable include fairly new transactions that do not have enough confirmations or
     * notes that are tied up because we are awaiting change from a transaction. When a note has
     * been spent, its change cannot be used until there are enough confirmations.
     */
    data class WalletBalance(
        val total: Long = -1,
        val available: Long = -1
    )

    //
    // Key Management Interfaces
    //

    interface KeyManager: SeedProvider, SpendingKeyStore, SpendingKeyProvider
    
    interface SeedProvider {
        val seed: ByteArray
    }
    interface SpendingKeyStore {
        var key: String
    }
    interface SpendingKeyProvider {
        val key: String
    }
}
