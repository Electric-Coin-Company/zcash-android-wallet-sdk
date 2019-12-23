package cash.z.wallet.sdk

import android.content.Context
import android.content.SharedPreferences
import cash.z.wallet.sdk.exception.BirthdayException
import cash.z.wallet.sdk.exception.BirthdayException.MissingBirthdayException
import cash.z.wallet.sdk.exception.InitializerException
import cash.z.wallet.sdk.ext.*
import cash.z.wallet.sdk.jni.RustBackend
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import java.io.InputStreamReader

/**
 * Responsible for initialization, which can be considered as setup that must happen before
 * synchronizing begins. This begins with one of three actions, a call to either [new], [import] or
 * [open], where the last option is the most common case--when a user is opening a wallet they have
 * used before on this device.
 */
class Initializer(
    appContext: Context,
    val host: String = ZcashSdk.DEFAULT_LIGHTWALLETD_HOST,
    val port: Int = ZcashSdk.DEFAULT_LIGHTWALLETD_PORT,
    private val alias: String = ZcashSdk.DEFAULT_DB_NAME_PREFIX
) {
    init {
        validateAlias(alias)
    }

    /**
     * The path this initializer will use when creating instances of Rustbackend. This value is
     * derived from the appContext when this class is constructed.
     */
    private val dbPath: String = appContext.getDatabasePath("unused.db").parentFile?.absolutePath
        ?: throw InitializerException.DatabasePathException

    /**
     * The path this initializer will use when cheching for and downloaading sapling params. This
     * value is derived from the appContext when this class is constructed.
     */
    private val paramPath: String = "${appContext.cacheDir.absolutePath}/params"

    /**
     * Preferences where the birthday is stored.
     */
    private val prefs: SharedPreferences = SharedPrefs(appContext, alias)

    /**
     * A wrapped version of [cash.z.wallet.sdk.jni.RustBackendWelding] that will be passed to the
     * SDK when it is constructed. It provides access to all Librustzcash features and is configured
     * based on this initializer.
     */
    lateinit var rustBackend: RustBackend

    /**
     * The birthday that was ultimately used for initializing the accounts.
     */
    lateinit var birthday: WalletBirthday

    /**
     * Birthday that helps new wallets not have to scan from the beginning, which saves significant
     * amounts of startup time. This value is created using the context passed into the constructor.
     */
    private var newWalletBirthday: WalletBirthday = loadBirthdayFromAssets(appContext)

    /**
     * Birthday to use whenever no birthday is known, meaning we have to scan from the first time a
     * transaction could have happened. This is the most efficient value we can use in this least
     * efficient circumstance. This value is created using the context passed into the constructor
     * and it is a different value for mainnet and testnet.
     */
    private var saplingBirthday: WalletBirthday =
        loadBirthdayFromAssets(appContext, ZcashSdk.SAPLING_ACTIVATION_HEIGHT)

    /**
     * Typically the first function that is called on this class in order to determine whether the
     * user needs to create a new wallet.
     *
     * @return true when an initialized wallet exists on this device.
     */
    fun hasData() = prefs.get<Boolean>(PREFS_HAS_DATA) == true

    /**
     * Initialize a new wallet with the given seed and birthday. It creates the required database
     * tables and loads and configures the [rustBackend] property for use by all other components.
     *
     * @return the account spending keys, corresponding to the accounts that get initialized in the
     * DB.
     */
    fun new(
        seed: ByteArray,
        birthday: WalletBirthday = newWalletBirthday,
        numberOfAccounts: Int = 1,
        overwrite: Boolean = false
    ): Array<String> {
        initRustLibrary()
        return initializeAccounts(seed, birthday, numberOfAccounts, overwrite)
    }

   /**
     * Initialize a new wallet with the imported seed and birthday. It creates the required database
     * tables and loads and configures the [rustBackend] property for use by all other components.
     *
     * @return the account spending keys, corresponding to the accounts that get initialized in the
     * DB.
     */
    fun import(
        seed: ByteArray,
        birthday: WalletBirthday = saplingBirthday,
        overwrite: Boolean = false
    ): Array<String> {
        initRustLibrary()
        return initializeAccounts(seed, birthday, overwrite = overwrite)
    }

    /**
     * Loads the rust library and previously used birthday for use by all other components. This is
     * the most common use case for the initializer--reopening a wallet that was previously created.
     */
    fun open(): Initializer {
        initRustLibrary()
        birthday = loadBirthdayFromPrefs(prefs) ?: throw MissingBirthdayException(alias)
        rustBackend.birthdayHeight = birthday.height
        return this
    }

    /**
     * Initializes the databases that the rust library uses for managing state. The "data db" is
     * created and a row is entered corresponding to the given birthday so that scanning does not
     * need to start from the  beginning of time. Lastly, the accounts table is initialized to
     * simply hold the address and viewing key for each account, which simplifies the process of
     * scanning and decrypting compact blocks.
     *
     * @return the spending keys for each account, ordered by index. These keys are only needed for
     * spending funds.
     */
    private fun initializeAccounts(
        seed: ByteArray,
        birthday: WalletBirthday = newWalletBirthday,
        numberOfAccounts: Int = 1,
        overwrite: Boolean = false
    ): Array<String> {
        this.birthday = birthday

        try {
            if (overwrite) rustBackend.clear()
            // only creates tables, if they don't exist
            rustBackend.initDataDb()
            twig("Initialized wallet for first run")
        } catch (t: Throwable) {
            throw InitializerException.FalseStart(t)
        }

        try {
            rustBackend.initBlocksTable(
                birthday.height,
                birthday.hash,
                birthday.time,
                birthday.tree
            )
            twig("seeded the database with sapling tree at height ${birthday.height}")
        } catch (t: Throwable) {
            if (t.message?.contains("is not empty") == true) {
                throw InitializerException.AlreadyInitializedException(t, rustBackend.dbDataPath)
            } else {
                throw InitializerException.FalseStart(t)
            }
        }

        try {
            return rustBackend.initAccountsTable(seed, numberOfAccounts).also {
                twig("Initialized the accounts table with ${numberOfAccounts} account(s)")
                onAccountsInitialized()
            }
        } catch (t: Throwable) {
            throw InitializerException.FalseStart(t)
        }
    }

    /**
     * Delete all local data related to this wallet, as though the wallet was never created on this
     * device. Simply put, this call deletes the "cache db" and "data db."
     */
    fun clear() {
        rustBackend.clear()
        prefs[PREFS_HAS_DATA] = false
    }

    /**
     * Validate that the alias doesn't contain malicious characters by enforcing simple rules which
     * permit the alias to be used as part of a file name for the preferences and databases. This
     * enables multiple wallets to exist on one device, which is also helpful for sweeping funds.
     *
     * @throws IllegalArgumentException whenever the alias is not less than 100 characters or
     * contains something other than alphanumeric characters. Underscores are allowed but aliases
     * must start with a letter.
     */
    private fun validateAlias(alias: String) {
        require(alias.length in 1..99 && alias[0].isLetter()
                && alias.all{ it.isLetterOrDigit() || it == '_' }) {
            "ERROR: Invalid alias ($alias). For security, the alias must be shorter than 100 " +
            "characters and only contain letters, digits or underscores and start with a letter."
        }
    }

    /**
     * Called when accounts have been successfully initialized. Stores the birthday and a flag to
     * signal that initialization has happened for the given alias.
     */
    private fun onAccountsInitialized() {
        saveBirthdayToPrefs(prefs, birthday)
        prefs[PREFS_HAS_DATA] = true

    }

    /**
     * Lazily initializes the rust backend, using values that were captured from the appContext
     * that was passed to the constructor.
     */
    private fun initRustLibrary() {
        if (!::rustBackend.isInitialized) rustBackend = RustBackend().init(dbPath, paramPath, alias)
    }


    //
    // Key Derivation Helpers
    //

    /**
     * Given a seed and a number of accounts, return the associated spending keys. These keys can
     * be used to derive the viewing keys.
     *
     * @return the spending keys that correspond to the seed, formatted as Strings.
     */
    fun deriveSpendingKeys(seed: ByteArray, numberOfAccounts: Int =  1): Array<String> {
        initRustLibrary()
        return rustBackend.deriveSpendingKeys(seed, numberOfAccounts)
    }

    /**
     * Given a seed and a number of accounts, return the associated viewing keys.
     *
     * @return the viewing keys that correspond to the seed, formatted as Strings.
     */
    fun deriveViewingKeys(seed: ByteArray, numberOfAccounts: Int =  1): Array<String> {
        initRustLibrary()
        return rustBackend.deriveViewingKeys(seed, numberOfAccounts)
    }

    /**
     * Given a spending key, return the associated viewing key.
     *
     * @return the viewing key that corresponds to the spending key.
     */
    fun deriveViewingKey(spendingKey: String): String = rustBackend.deriveViewingKey(spendingKey)


    /**
     * Static helper functions that facilitate initializing the birthday.
     */
    companion object {

        //
        // Preference Keys
        //

        private const val PREFS_HAS_DATA = "Initializer.prefs.hasData"
        private const val PREFS_BIRTHDAY_HEIGHT = "Initializer.prefs.birthday.height"
        private const val PREFS_BIRTHDAY_TIME = "Initializer.prefs.birthday.time"
        private const val PREFS_BIRTHDAY_HASH = "Initializer.prefs.birthday.hash"
        private const val PREFS_BIRTHDAY_TREE = "Initializer.prefs.birthday.tree"


        /**
         * Directory within the assets folder where birthday data
         * (i.e. sapling trees for a given height) can be found.
         */
        private const val BIRTHDAY_DIRECTORY = "zcash/saplingtree"

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
            if (treeFiles.isNullOrEmpty()) throw BirthdayException.MissingBirthdayFilesException(
                BIRTHDAY_DIRECTORY
            )
            val file: String
            try {
                file = if (birthdayHeight == null) treeFiles.first() else {
                    treeFiles.first {
                        it.split(".").first().toInt() <= birthdayHeight
                    }
                }
            } catch (t: Throwable) {
                throw BirthdayException.BirthdayFileNotFoundException(
                    BIRTHDAY_DIRECTORY,
                    birthdayHeight
                )
            }
            try {
                val reader = JsonReader(
                    InputStreamReader(context.assets.open("${BIRTHDAY_DIRECTORY}/$file"))
                )
                return Gson().fromJson(reader, WalletBirthday::class.java)
            } catch (t: Throwable) {
                throw BirthdayException.MalformattedBirthdayFilesException(
                    BIRTHDAY_DIRECTORY,
                    treeFiles[0]
                )
            }
        }

        /**
         * Retrieves the birthday-related primitives from the given preference object and then uses
         * them to construct and return a birthday instance. It assumes that if the first preference
         * is there, the rest will be too. If that's not the case, a call to this function will
         * result in an exception.
         *
         * @return a birthday from preferences if one exists and null, otherwise null
         */
        fun loadBirthdayFromPrefs(prefs: SharedPreferences?): WalletBirthday? {
            prefs ?: return null
            val height: Int? = prefs[PREFS_BIRTHDAY_HEIGHT]
            return height?.let {
                runCatching {
                    WalletBirthday(
                        it,
                        prefs[PREFS_BIRTHDAY_HASH]!!,
                        prefs[PREFS_BIRTHDAY_TIME]!!,
                        prefs[PREFS_BIRTHDAY_TREE]!!
                    )
                }.getOrNull()
            }
        }

        /**
         * Save the given birthday to the given preferences.
         *
         * @param prefs the shared preferences to use
         * @param birthday the birthday to save. It will be split into primitives.
         */
        fun saveBirthdayToPrefs(prefs: SharedPreferences, birthday: WalletBirthday) {
            twig("saving birthday to prefs (${birthday.height})")
            prefs[PREFS_BIRTHDAY_HEIGHT] = birthday.height
            prefs[PREFS_BIRTHDAY_HASH] = birthday.hash
            prefs[PREFS_BIRTHDAY_TIME] = birthday.time
            prefs[PREFS_BIRTHDAY_TREE] = birthday.tree
        }

    }

    /**
     * Model object for holding wallet birthdays. It is only used by this class.
     */
    data class WalletBirthday(
        val height: Int = -1,
        val hash: String = "",
        val time: Long = -1,
        val tree: String = ""
    )
}