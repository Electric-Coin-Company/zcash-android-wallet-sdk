package cash.z.wallet.sdk

import android.content.Context
import android.content.SharedPreferences
import cash.z.wallet.sdk.exception.BirthdayException
import cash.z.wallet.sdk.exception.InitializerException
import cash.z.wallet.sdk.ext.*
import cash.z.wallet.sdk.jni.RustBackend
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import java.io.File
import java.io.InputStreamReader
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Responsible for initialization, which can be considered as setup that must happen before
 * synchronizing begins. This begins with one of three actions, a call to either [new], [import] or
 * [open], where the last option is the most common case--when a user is opening a wallet they have
 * used before on this device.
 *
 * @param appContext the application context, used to extract the storage paths for the databases
 * and param files. A reference to the context is not held beyond initialization.
 * @param host the host that the synchronizer should use.
 * @param port the port that the synchronizer should use when connecting to the host.
 * @param alias the alias to use for this synchronizer. Think of it as a unique name that allows
 * multiple synchronizers to function in the same app. The alias is mapped to database names for the
 * cache and data DBs. This value is optional and is usually not required because most apps only
 * need one synchronizer.
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
     * The path this initializer will use when checking for and downloading sapling params. This
     * value is derived from the appContext when this class is constructed.
     */
    private val pathParams: String = "${appContext.cacheDir.absolutePath}/params"

    /**
     * The path used for storing cached compact blocks for processing.
     */
    private val pathCacheDb: String = cacheDbPath(appContext, alias)

    /**
     * The path used for storing the data derived from the cached compact blocks.
     */
    private val pathDataDb: String = dataDbPath(appContext, alias)

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
     * Returns true when either 'open', 'new' or 'import' have already been called. Each of those
     * functions calls `initRustLibrary` before returning. The entire point of the initializer is to
     * setup everything necessary for the Synchronizer to function, which mainly boils down to
     * loading the rust backend.
     */
    val isInitialized: Boolean get() = ::rustBackend.isInitialized

    /**
     * Initialize a new wallet with the given seed and birthday. It creates the required database
     * tables and loads and configures the [rustBackend] property for use by all other components.
     *
     * @param seed the seed to use for the newly created wallet.
     * @param newWalletBirthday the birthday to use for the newly created wallet. Typically, this
     * corresponds to the most recent checkpoint available since new wallets should not have any
     * transactions prior to their creation.
     * @param numberOfAccounts the number of accounts to create for this wallet. This is not fully
     * supported so the default value of 1 is recommended.
     * @param clearCacheDb when true, this will delete cacheDb, if it exists, resulting in the fresh
     * download of all compact blocks. Otherwise, downloading resumes from the last fetched block.
     * @param clearDataDb when true, this will delete the dataDb, if it exists, resulting in the
     * fresh scan of all blocks. Otherwise, initialization crashes when previous wallet data exists
     * to prevent accidental overwrites.
     *
     * @return the account spending keys, corresponding to the accounts that get initialized in the
     * DB.
     * @throws InitializerException.AlreadyInitializedException when the blocks table already exists
     * and [clearDataDb] is false.
     *
     * @return the spending key(s) associated with this wallet, for convenience.
     */
    fun new(
        seed: ByteArray,
        newWalletBirthday: WalletBirthday,
        numberOfAccounts: Int = 1,
        clearCacheDb: Boolean = false,
        clearDataDb: Boolean = false
    ): Array<String> {
        return initializeAccounts(seed, newWalletBirthday, numberOfAccounts,
            clearCacheDb = clearCacheDb, clearDataDb = clearDataDb)
    }

    /**
     * Initialize a new wallet with the imported seed and birthday. It creates the required database
     * tables and loads and configures the [rustBackend] property for use by all other components.
     *
     * @param seed the seed to use for the imported wallet.
     * @param previousWalletBirthday the birthday to use for the imported. Typically, this
     * corresponds to the height where this wallet was first created, allowing the wallet to be
     * optimized not to download or scan blocks from before the wallet existed.
     * @param clearCacheDb when true, this will delete cacheDb, if it exists, resulting in the fresh
     * download of all compact blocks. Otherwise, downloading resumes from the last fetched block.
     * @param clearDataDb when true, this will delete the dataDb, if it exists, resulting in the
     * fresh scan of all blocks. Otherwise, this function throws an exception when previous wallet
     * data exists to prevent accidental overwrites.
     *
     * @return the account spending keys, corresponding to the accounts that get initialized in the
     * DB.
     * @throws InitializerException.AlreadyInitializedException when the blocks table already exists
     * and [clearDataDb] is false.
     *
     * @return the spending key(s) associated with this wallet, for convenience.
     */
    fun import(
       seed: ByteArray,
       previousWalletBirthday: WalletBirthday,
       clearCacheDb: Boolean = false,
       clearDataDb: Boolean = false
    ): Array<String> {
        return initializeAccounts(seed, previousWalletBirthday,
            clearCacheDb = clearCacheDb, clearDataDb = clearDataDb)
    }

    /**
     * Loads the rust library and previously used birthday for use by all other components. This is
     * the most common use case for the initializer--reopening a wallet that was previously created.
     *
     * @param birthday birthday height of the wallet. This value is passed to the
     * [CompactBlockProcessor] and becomes a factor in determining the lower bounds height that this
     * wallet will use. This height helps with determining where to start downloading as well as how
     * far back to go during a rewind. Every wallet has a birthday and the initializer depends on
     * this value but does not own it.
     *
     * @return an instance of this class so that the function can be used fluidly. Spending keys are
     * not returned because the SDK does not store them and this function is for opening a wallet
     * that was created previously.
     */
    fun open(birthday: WalletBirthday): Initializer {
        twig("Opening wallet with birthday ${birthday.height}")
        requireRustBackend().birthdayHeight = birthday.height
        return this
    }

    /**
     * Initializes the databases that the rust library uses for managing state. The "data db" is
     * created and a row is entered corresponding to the given birthday so that scanning does not
     * need to start from the  beginning of time. Lastly, the accounts table is initialized to
     * simply hold the address and viewing key for each account, which simplifies the process of
     * scanning and decrypting compact blocks.
     *
     * @param seed the seed to use for initializing accounts. We derive the address and the viewing
     * key(s) from this seed and also return the related spending key(s). Only the viewing key is
     * retained in the database in order to simplify scanning for the wallet.
     * @param birthday the birthday to use for this wallet. This is used in order to seed the data
     * DB with the first sapling tree, which also determines where the SDK begins downloading and
     * scanning. Any blocks lower than the height represented by this birthday can safely be ignored
     * since a wallet cannot have transactions prior to it's creation.
     * @param numberOfAccounts the number of accounts to create. Only 1 account is tested and
     * supported at this time. It is possible, although unlikely that multiple accounts would behave
     * as expected. Due to the nature of shielded address, the official Zcash recommendation is to
     * only use one address for shielded transactions. Unlike transparent coins, address rotation is
     * not necessary for shielded Zcash transactions because the sensitive information is private.
     * @param clearCacheDb when true, the cache DB will be deleted prior to initializing accounts.
     * This is useful for preventing errors when the database already exists, which happens often
     * in tests, demos and proof of concepts.
     * @param clearDataDb when true, the cache DB will be deleted prior to initializing accounts.
     * This is useful for preventing errors when the database already exists, which happens often
     * in tests, demos and proof of concepts.
     *
     * @return the spending keys for each account, ordered by index. These keys are only needed for
     * spending funds.
     */
    private fun initializeAccounts(
        seed: ByteArray,
        birthday: WalletBirthday,
        numberOfAccounts: Int = 1,
        clearCacheDb: Boolean = false,
        clearDataDb: Boolean = false
    ): Array<String> {
        this.birthday = birthday
        twig("Initializing accounts with birthday ${birthday.height}")
        try {
            requireRustBackend().clear(clearCacheDb, clearDataDb)
            // only creates tables, if they don't exist
            requireRustBackend().initDataDb()
            twig("Initialized wallet for first run")
        } catch (t: Throwable) {
            throw InitializerException.FalseStart(t)
        }

        try {
            requireRustBackend().initBlocksTable(
                birthday.height,
                birthday.hash,
                birthday.time,
                birthday.tree
            )
            twig("seeded the database with sapling tree at height ${birthday.height}")
        } catch (t: Throwable) {
            if (t.message?.contains("is not empty") == true) {
                throw InitializerException.AlreadyInitializedException(t, rustBackend.pathDataDb)
            } else {
                throw InitializerException.FalseStart(t)
            }
        }

        try {
            return requireRustBackend().initAccountsTable(seed, numberOfAccounts).also {
                twig("Initialized the accounts table with ${numberOfAccounts} account(s)")
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
    }

    /**
     * Internal function used to initialize the [rustBackend] before use. Initialization should only
     * happen as a result of [new], [import] or [open] being called or as part of stand-alone key
     * derivation. This involves loading the shared object file via `System.loadLibrary`.
     *
     * @return the rustBackend that was loaded by this initializer.
     */
    private fun requireRustBackend(): RustBackend {
        if (!isInitialized) {
            twig("Initializing cache: $pathCacheDb  data: $pathDataDb  params: $pathParams")
            rustBackend = RustBackend().init(pathCacheDb, pathDataDb, pathParams)
        }
        return rustBackend
    }


    //
    // Key Derivation Helpers
    //

    /**
     * Given a seed and a number of accounts, return the associated spending keys. These keys can
     * be used to derive the viewing keys.
     *
     * @param seed the seed from which to derive spending keys.
     * @param numberOfAccounts the number of accounts to use. Multiple accounts are not fully
     * supported so the default value of 1 is recommended.
     *
     * @return the spending keys that correspond to the seed, formatted as Strings.
     */
    fun deriveSpendingKeys(seed: ByteArray, numberOfAccounts: Int =  1): Array<String> =
        requireRustBackend().deriveSpendingKeys(seed, numberOfAccounts)

    /**
     * Given a seed and a number of accounts, return the associated viewing keys.
     *
     * @param seed the seed from which to derive viewing keys.
     * @param numberOfAccounts the number of accounts to use. Multiple accounts are not fully
     * supported so the default value of 1 is recommended.
     *
     * @return the viewing keys that correspond to the seed, formatted as Strings.
     */
    fun deriveViewingKeys(seed: ByteArray, numberOfAccounts: Int =  1): Array<String> =
        requireRustBackend().deriveViewingKeys(seed, numberOfAccounts)

    /**
     * Given a spending key, return the associated viewing key.
     *
     * @param spendingKey the key from which to derive the viewing key.
     *
     * @return the viewing key that corresponds to the spending key.
     */
    fun deriveViewingKey(spendingKey: String): String =
        requireRustBackend().deriveViewingKey(spendingKey)

    /**
     * Given a seed and account index, return the associated address.
     *
     * @param seed the seed from which to derive the address.
     * @param accountIndex the index of the account to use for deriving the address. Multiple
     * accounts are not fully supported so the default value of 1 is recommended.
     *
     * @return the address that corresponds to the seed and account index.
     */
    fun deriveAddress(seed: ByteArray, accountIndex: Int = 0) =
        requireRustBackend().deriveAddress(seed, accountIndex)

    /**
     * Given a viewing key string, return the associated address.
     *
     * @param viewingKey the viewing key to use for deriving the address. The viewing key is tied to
     * a specific account so no account index is required.
     *
     * @return the address that corresponds to the viewing key.
     */
    fun deriveAddress(viewingKey: String) =
        requireRustBackend().deriveAddress(viewingKey)


    companion object {

        //
        // Path Helpers
        //

        /**
         * Returns the path to the cache database that would correspond to the given alias.
         *
         * @param appContext the application context
         * @param alias the alias to convert into a database path
         */
        fun cacheDbPath(appContext: Context, alias: String): String =
            aliasToPath(appContext, alias, ZcashSdk.DB_CACHE_NAME)

        /**
         * Returns the path to the data database that would correspond to the given alias.
         * @param appContext the application context
         * @param alias the alias to convert into a database path
         */
        fun dataDbPath(appContext: Context, alias: String): String =
            aliasToPath(appContext, alias, ZcashSdk.DB_DATA_NAME)

        private fun aliasToPath(appContext: Context, alias: String, dbFileName: String): String {
            val parentDir: String =
                appContext.getDatabasePath("unused.db").parentFile?.absolutePath
                    ?: throw InitializerException.DatabasePathException
            val prefix = if (alias.endsWith('_')) alias else "${alias}_"
            return File(parentDir, "$prefix$dbFileName").absolutePath
        }
    }


    /**
     * Model object for holding a wallet birthday. It is only used by this class.
     *
     * @param height the height at the time the wallet was born.
     * @param hash the hash of the block at the height.
     * @param time the block time at the height.
     * @param tree the sapling tree corresponding to the height.
     */
    data class WalletBirthday(
        val height: Int = -1,
        val hash: String = "",
        val time: Long = -1,
        val tree: String = ""
    )

    /**
     * Interface for classes that can handle birthday storage. This makes it possible to bridge into
     * existing storage logic. Instances of this interface can also be used as property delegates,
     * which enables the syntax `val birthday by birthdayStore`
     */
    interface WalletBirthdayStore : ReadWriteProperty<R, WalletBirthday> {
        val newWalletBirthday: WalletBirthday

        /**
         * Get the birthday of the wallet, saved in this store.
         */
        fun getBirthday(): WalletBirthday

        /**
         * Set the birthday of the wallet to be saved in this store.
         */
        fun setBirthday(value: WalletBirthday)

        /**
         * Load a birthday matching the given height. This is most commonly used during import to
         * find the first available checkpoint that is lower than the requested height.
         *
         * @param birthdayHeight the height to use as an upper bound for loading.
         */
        fun loadBirthday(birthdayHeight: Int): WalletBirthday

        /**
         * Return true when a birthday has been stored in this instance.
         */
        fun hasExistingBirthday(): Boolean

        /**
         * Return true when a birthday was imported into this instance.
         */
        fun hasImportedBirthday(): Boolean

        /* Property implementation that allows this interface to be used as a property delegate */

        /**
         * Implement readable interface in order to be able to use instances of this interface as
         * property delegates.
         */
        override fun getValue(thisRef: R, property: KProperty<*>): WalletBirthday {
            return getBirthday()
        }

        /**
         * Implement writable interface in order to be able to use instances of this interface as
         * property delegates.
         */
        override fun setValue(thisRef: R, property: KProperty<*>, value: WalletBirthday) {
            setBirthday(value)
        }

    }

    /**
     * Default implementation of the [WalletBirthdayStore] interface that loads checkpoints from the
     * assets directory, in JSON format and stores the current birthday in shared preferences.
     */
    class DefaultBirthdayStore(
        private val appContext: Context,
        private val importedBirthdayHeight: Int? = null,
        val alias: String = DEFAULT_ALIAS
    ) : WalletBirthdayStore {

        /**
         * Birthday that helps new wallets not have to scan from the beginning, which saves
         * significant amounts of startup time. This value is created using the context passed into
         * the constructor.
         */
        override val newWalletBirthday: WalletBirthday get() = loadBirthdayFromAssets(appContext)

        /**
         * Birthday to use whenever no birthday is known, meaning we have to scan from the first
         * time a transaction could have happened. This is the most efficient value we can use in
         * this least efficient circumstance. This value is created using the context passed into
         * the constructor and it is a different value for mainnet and testnet.
         */
        private val saplingBirthday: WalletBirthday get() =
            loadBirthdayFromAssets(appContext, ZcashSdk.SAPLING_ACTIVATION_HEIGHT)

        /**
         * Preferences where the birthday is stored.
         */
        private val prefs: SharedPreferences = SharedPrefs(appContext, alias)

        init {
            validateAlias(alias)
        }

        override fun hasExistingBirthday(): Boolean = loadBirthdayFromPrefs(prefs) != null

        override fun hasImportedBirthday(): Boolean = importedBirthdayHeight != null

        override fun getBirthday(): Initializer.WalletBirthday {
            return loadBirthdayFromPrefs(prefs).apply { twig("Loaded birthday from prefs: ${this?.height}") } ?: saplingBirthday.apply { twig("returning sapling birthday") }
        }

        override fun setBirthday(value: WalletBirthday) {
            twig("Setting birthday to ${value.height}")
            saveBirthdayToPrefs(prefs, value)
        }

        override fun loadBirthday(birthdayHeight: Int) =
            loadBirthdayFromAssets(appContext, birthdayHeight)

        /**
         * Retrieves the birthday-related primitives from the given preference object and then uses
         * them to construct and return a birthday instance. It assumes that if the first preference
         * is there, the rest will be too. If that's not the case, a call to this function will
         * result in an exception.
         *
         * @param prefs the shared preference to use for loading the birthday.
         *
         * @return a birthday from preferences if one exists and null, otherwise null
         */
        private fun loadBirthdayFromPrefs(prefs: SharedPreferences?): WalletBirthday? {
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
         * @param prefs the shared preferences to use for saving the birthday.
         * @param birthday the birthday to save. It will be split into primitives.
         */
        private fun saveBirthdayToPrefs(prefs: SharedPreferences, birthday: WalletBirthday) {
            twig("saving birthday to prefs (${birthday.height})")
            prefs[PREFS_BIRTHDAY_HEIGHT] = birthday.height
            prefs[PREFS_BIRTHDAY_HASH] = birthday.hash
            prefs[PREFS_BIRTHDAY_TIME] = birthday.time
            prefs[PREFS_BIRTHDAY_TREE] = birthday.tree
        }

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
             * The default alias to use for naming the preference file used for storage.
             */
            const val DEFAULT_ALIAS = "default_prefs"

            /**
             * A convenience constructor function for creating an instance of this class to use for
             * new wallets. It sets the stored birthday to match the `newWalletBirthday` checkpoint
             * which is typically the most recent checkpoint available.
             *
             * @param appContext the application context.
             * @param alias the alias to use when naming the preferences file used for storage.
             */
            fun NewWalletBirthdayStore(appContext: Context, alias: String = DEFAULT_ALIAS): WalletBirthdayStore {
                return DefaultBirthdayStore(appContext, alias = alias).apply {
                    setBirthday(newWalletBirthday)
                }
            }

            /**
             * A convenience constructor function for creating an instance of this class to use for
             * imported wallets. It sets the stored birthday to match the given
             * `importedBirthdayHeight` by finding the highest checkpoint that is below that height.
             *
             * @param appContext the application context.
             * @param importedBirthdayHeight the height corresponding to the birthday of the wallet
             * being imported. A checkpoint will be generated that allows scanning to start as close
             * to this height as possible because any blocks before this height can safely be
             * ignored since a wallet cannot have transactions before it is born.
             * @param alias the alias to use when naming the preferences file used for storage.
             */
            fun ImportedWalletBirthdayStore(appContext: Context, importedBirthdayHeight: Int?, alias: String = DEFAULT_ALIAS): WalletBirthdayStore {
                return DefaultBirthdayStore(appContext, alias = alias).apply {
                    if (importedBirthdayHeight != null) {
                        saveBirthdayToPrefs(prefs, loadBirthdayFromAssets(appContext, importedBirthdayHeight))
                    } else {
                        setBirthday(newWalletBirthday)
                    }
                }
            }

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
            fun loadBirthdayFromAssets(
                context: Context,
                birthdayHeight: Int? = null
            ): WalletBirthday {
                twig("loading birthday from assets: $birthdayHeight")
                val treeFiles =
                    context.assets.list(BIRTHDAY_DIRECTORY)?.apply { sortByDescending { fileName ->
                        try {
                            fileName.split('.').first().toInt()
                        } catch (t: Throwable) {
                            ZcashSdk.SAPLING_ACTIVATION_HEIGHT
                        }
                    } }
                if (treeFiles.isNullOrEmpty()) throw BirthdayException.MissingBirthdayFilesException(
                    BIRTHDAY_DIRECTORY
                )
                twig("found ${treeFiles.size} sapling tree checkpoints: ${Arrays.toString(treeFiles)}")
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
        }
    }
}

/**
 * Validate that the alias doesn't contain malicious characters by enforcing simple rules which
 * permit the alias to be used as part of a file name for the preferences and databases. This
 * enables multiple wallets to exist on one device, which is also helpful for sweeping funds.
 *
 * @param alias the alias to validate.
 *
 * @throws IllegalArgumentException whenever the alias is not less than 100 characters or
 * contains something other than alphanumeric characters. Underscores are allowed but aliases
 * must start with a letter.
 */
internal fun validateAlias(alias: String) {
    require(alias.length in 1..99 && alias[0].isLetter()
            && alias.all{ it.isLetterOrDigit() || it == '_' }) {
        "ERROR: Invalid alias ($alias). For security, the alias must be shorter than 100 " +
                "characters and only contain letters, digits or underscores and start with a letter"
    }
}