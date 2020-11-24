package cash.z.ecc.android.sdk

import android.content.Context
import cash.z.ecc.android.sdk.exception.InitializerException
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.ZcashSdk.SAPLING_ACTIVATION_HEIGHT
import cash.z.ecc.android.sdk.ext.tryWarn
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool
import java.io.File

/**
 * Simplified Initializer focused on starting from a ViewingKey.
 */
class Initializer constructor(appContext: Context, config: Config):  SdkSynchronizer.SdkInitializer {
    override val context = appContext.applicationContext
    override val rustBackend: RustBackend
    override val alias: String
    override val host: String
    override val port: Int
    val viewingKeys: List<String>
    val birthday: WalletBirthdayTool.WalletBirthday

    /**
     * True when accounts have been created by this initializer.
     *
     * NOTE: This is a code smell that the initializer should not be creating databases but that
     * will be addressed in the next iteration and/or when the Data Access API is implemented
     */
    var accountsCreated = false

    init {
        config.validate()
        val heightToUse = config.birthdayHeight
            ?: (if (config.defaultToOldestHeight == true) SAPLING_ACTIVATION_HEIGHT else null)
        val loadedBirthday = WalletBirthdayTool.loadNearest(context, heightToUse)
        birthday = loadedBirthday
        viewingKeys = config.viewingKeys
        alias = config.alias
        host = config.host
        port = config.port
        rustBackend = initRustBackend(birthday)
        // TODO: get rid of this by first answering the question: why is this necessary?
        initMissingDatabases(birthday, *viewingKeys.toTypedArray())
    }

    constructor(appContext: Context, block: (Config) -> Unit) : this(appContext, Config(block))

    fun erase() = erase(context, alias)

    private fun initRustBackend(birthday: WalletBirthdayTool.WalletBirthday): RustBackend {
        return RustBackend.init(
            cacheDbPath(context, alias),
            dataDbPath(context, alias),
            "${context.cacheDir.absolutePath}/params",
            birthday.height
        )
    }

    private fun initMissingDatabases(
        birthday: WalletBirthdayTool.WalletBirthday,
        vararg viewingKeys: String
    ) {
        maybeCreateDataDb()
        maybeInitBlocksTable(birthday)
        maybeInitAccountsTable(*viewingKeys)
    }

    /**
     * Create the dataDb and its table, if it doesn't exist.
     */
    private fun maybeCreateDataDb() {
        tryWarn("Warning: did not create dataDb. It probably already exists.") {
            rustBackend.initDataDb()
            twig("Initialized wallet for first run")
        }
    }

    /**
     * Initialize the blocks table with the given birthday, if needed.
     */
    private fun maybeInitBlocksTable(birthday: WalletBirthdayTool.WalletBirthday) {
        tryWarn(
            "Warning: did not initialize the blocks table. It probably was already initialized."
        ) {
            rustBackend.initBlocksTable(
                birthday.height,
                birthday.hash,
                birthday.time,
                birthday.tree
            )
            twig("seeded the database with sapling tree at height ${birthday.height}")
        }
    }

    /**
     * Initialize the accounts table with the given viewing keys, if needed.
     */
    private fun maybeInitAccountsTable(vararg viewingKeys: String) {
        tryWarn(
            "Warning: did not initialize the accounts table. It probably was already initialized."
        ) {
            rustBackend.initAccountsTable(*viewingKeys)
            accountsCreated = true
            twig("Initialized the accounts table with ${viewingKeys.size} viewingKey(s)")
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
                && alias.all { it.isLetterOrDigit() || it == '_' }) {
            "ERROR: Invalid alias ($alias). For security, the alias must be shorter than 100 " +
                    "characters and only contain letters, digits or underscores and start with a letter"
        }
    }


    class Config private constructor (
        val viewingKeys: MutableList<String> = mutableListOf(),
        var alias: String = ZcashSdk.DEFAULT_ALIAS,
        var host: String = ZcashSdk.DEFAULT_LIGHTWALLETD_HOST,
        var port: Int = ZcashSdk.DEFAULT_LIGHTWALLETD_PORT,
    ) {
        var birthdayHeight: Int? = null
            private set

        /**
         * Determines the default behavior for null birthdays. When null, nothing has been specified
         * so a null birthdayHeight value is an error. When false, null birthdays will be replaced
         * with the most recent checkpoint height available (typically, the latest `*.json` file in
         * `assets/zcash/saplingtree/`). When true, null birthdays will be replaced with the oldest
         * reasonable height where a transaction could exist (typically, sapling activation but
         * better approximations could be devised in the future, such as the date when the first
         * BIP-39 zcash wallets came online).
         */
        var defaultToOldestHeight: Boolean? = null
            private set

        constructor(block: (Config) -> Unit) : this() {
            block(this)
        }


        //
        // Birthday functions
        //

        /**
         * Set the birthday height for this configuration. When the height is not known, the wallet
         * can either default to the latest known birthday (in order to sync new wallets faster) or
         * the oldest possible birthday (in order to import a wallet with an unknown birthday
         * without skipping old transactions).
         *
         * @param height nullable birthday height to use for this configuration.
         * @param defaultToOldestHeight determines how a null birthday height will be
         * interpreted. Typically, `false` for new wallets and `true` for restored wallets because
         * new wallets want to load quickly but restored wallets want to find all possible
         * transactions.
         *
         */
        fun setBirthdayHeight(height: Int?, defaultToOldestHeight: Boolean = false): Config =
            apply {
                this.birthdayHeight = height
                this.defaultToOldestHeight = defaultToOldestHeight
            }

        /**
         * Load the most recent checkpoint available. This is useful for new wallets.
         */
        fun newWalletBirthday() {
            birthdayHeight = null
            defaultToOldestHeight = false
        }

        /**
         * Load the birthday checkpoint closest to the given wallet birthday. This is useful when
         * importing a pre-existing wallet. It is the same as calling
         * `birthdayHeight = importedHeight`.
         */
        fun importedWalletBirthday(importedHeight: Int) {
            birthdayHeight = importedHeight
            defaultToOldestHeight = true
        }


        //
        // Viewing key functions
        //

        /**
         * Add viewing keys to the set of accounts to monitor. Note: Using more than one viewing key
         * is not currently well supported. Consider it an alpha-preview feature that might work but
         * probably has serious bugs.
         */
        fun setViewingKeys(vararg extendedFullViewingKeys: String): Config = apply {
            viewingKeys.apply {
                clear()
                addAll(extendedFullViewingKeys)
            }
        }

        /**
         * Add viewing key to the set of accounts to monitor. Note: Using more than one viewing key
         * is not currently well supported. Consider it an alpha-preview feature that might work but
         * probably has serious bugs.
         */
        fun addViewingKey(extendedFullViewingKey: String): Config = apply {
            viewingKeys.add(extendedFullViewingKey)
        }


        //
        // Convenience functions
        //

        fun server(host: String, port: Int) {
            this.host = host
            this.port = port
        }

        fun import(seed: ByteArray, birthdayHeight: Int) {
            setSeed(seed)
            importedWalletBirthday(birthdayHeight)
        }

        fun import(
            viewingKey: String,
            birthdayHeight: Int,
            host: String = ZcashSdk.DEFAULT_LIGHTWALLETD_HOST,
            port: Int = ZcashSdk.DEFAULT_LIGHTWALLETD_PORT
        ) {
            setViewingKeys(viewingKey)
            server(host, port)
            this.birthdayHeight = birthdayHeight
        }

        fun new(
            seed: ByteArray,
            host: String = ZcashSdk.DEFAULT_LIGHTWALLETD_HOST,
            port: Int = ZcashSdk.DEFAULT_LIGHTWALLETD_PORT
        ) {
            setSeed(seed)
            server(host, port)
            newWalletBirthday()
        }

        fun new(
            viewingKey: String,
            host: String = ZcashSdk.DEFAULT_LIGHTWALLETD_HOST,
            port: Int = ZcashSdk.DEFAULT_LIGHTWALLETD_PORT
        ) {
            setViewingKeys(viewingKey)
            server(host, port)
            newWalletBirthday()
        }

        /**
         * Convenience method for setting thew viewingKeys from a given seed. This is the same as
         * calling `setViewingKeys` with the keys that match this seed.
         */
        fun setSeed(seed: ByteArray, numberOfAccounts: Int = 1) {
            setViewingKeys(*DerivationTool.deriveViewingKeys(seed, numberOfAccounts))
        }


        //
        // Validation helpers
        //

        fun validate() {
            validateAlias(alias)
            validateViewingKeys()
            validateBirthday()
        }

        private fun validateBirthday() {
            // if birthday is missing then we need to know how to interpret it
            // so defaultToOldestHeight ought to be set, in that case
            if (birthdayHeight == null && defaultToOldestHeight == null) {
                throw InitializerException.MissingDefaultBirthdayException
            }
            // allow either null or a value greater than the activation height
            if (
                (birthdayHeight ?: SAPLING_ACTIVATION_HEIGHT)
                < SAPLING_ACTIVATION_HEIGHT
            ) {
                throw InitializerException.InvalidBirthdayHeightException(birthdayHeight)
            }
        }

        private fun validateViewingKeys() {
            require(viewingKeys.isNotEmpty()) {
                "Viewing keys are required. Ensure that the viewing keys or seed have been set" +
                        " on this Initializer."
            }
            viewingKeys.forEach {
                DerivationTool.validateViewingKey(it)
            }
        }
    }

    companion object : SdkSynchronizer.Erasable {

        /**
         * Delete the databases associated with this wallet. This removes all compact blocks and
         * data derived from those blocks. For most wallets, this should not result in a loss of
         * funds because the seed and spending keys are stored separately. This call just removes
         * the associated data but not the seed or spending key, themselves, because those are
         * managed separately by the wallet.
         *
         * @param appContext the application context.
         * @param alias the alias used to create the local data.
         *
         * @return true when associated files were found. False most likely indicates that the wrong
         * alias was provided.
         */
        override fun erase(appContext: Context, alias: String) =
            delete(cacheDbPath(appContext, alias)) || delete(dataDbPath(appContext, alias))


        //
        // Path Helpers
        //

        /**
         * Returns the path to the cache database that would correspond to the given alias.
         *
         * @param appContext the application context
         * @param alias the alias to convert into a database path
         */
        internal fun cacheDbPath(appContext: Context, alias: String): String =
            aliasToPath(appContext, alias, ZcashSdk.DB_CACHE_NAME)

        /**
         * Returns the path to the data database that would correspond to the given alias.
         * @param appContext the application context
         * @param alias the alias to convert into a database path
         */
        internal fun dataDbPath(appContext: Context, alias: String): String =
            aliasToPath(appContext, alias, ZcashSdk.DB_DATA_NAME)

        private fun aliasToPath(appContext: Context, alias: String, dbFileName: String): String {
            val parentDir: String =
                appContext.getDatabasePath("unused.db").parentFile?.absolutePath
                    ?: throw InitializerException.DatabasePathException
            val prefix = if (alias.endsWith('_')) alias else "${alias}_"
            return File(parentDir, "$prefix$dbFileName").absolutePath
        }

        /**
         * Delete the file at the given path.
         *
         * @param filePath the path of the file to erase.
         * @return true when a file exists at the given path and was deleted.
         */
        private fun delete(filePath: String) : Boolean {
            return File(filePath).let {
                if (it.exists()) {
                    twig("Deleting ${it.name}!")
                    it.delete()
                    true
                } else {
                    false
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
