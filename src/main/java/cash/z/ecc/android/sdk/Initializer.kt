package cash.z.ecc.android.sdk

import android.content.Context
import cash.z.ecc.android.sdk.exception.InitializerException
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.tryWarn
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool
import java.io.File

/**
 * Simplified Initializer focused on starting from a ViewingKey.
 */
class Initializer constructor(appContext: Context, builder: Builder):  SdkSynchronizer.SdkInitializer {
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
        val loadedBirthday =
            builder.birthday ?: WalletBirthdayTool.loadNearest(context, builder.birthdayHeight)
        birthday = loadedBirthday
        viewingKeys = builder.viewingKeys
        alias = builder.alias
        host = builder.host
        port = builder.port
        rustBackend = initRustBackend(birthday)
        // TODO: get rid of this by first answering the question: why is this necessary?
        initMissingDatabases(birthday, *viewingKeys.toTypedArray())
    }

    constructor(appContext: Context, block: (Builder) -> Unit) : this(appContext, Builder(block))

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


    class Builder private constructor(
         val viewingKeys: MutableList<String> = mutableListOf(),
         var birthday: WalletBirthdayTool.WalletBirthday? = null,
         var birthdayHeight: Int? = null,
         var alias: String = ZcashSdk.DEFAULT_ALIAS,
         var host: String = ZcashSdk.DEFAULT_LIGHTWALLETD_HOST,
         var port: Int = ZcashSdk.DEFAULT_LIGHTWALLETD_PORT
    ) {
        constructor(block: (Builder) -> Unit) : this(mutableListOf(), null, null) {
            block(this)
            validate()
        }
        constructor(
            viewingKeys: MutableList<String> = mutableListOf(),
            birthday: WalletBirthdayTool.WalletBirthday? = null,
            /* optional fields with default values */
            alias: String = ZcashSdk.DEFAULT_ALIAS,
            host: String = ZcashSdk.DEFAULT_LIGHTWALLETD_HOST,
            port: Int = ZcashSdk.DEFAULT_LIGHTWALLETD_PORT
        ) : this(viewingKeys, birthday, -1, alias, host, port) {
            validate()
        }

        constructor(
            viewingKeys: MutableList<String> = mutableListOf(),
            birthdayHeight: Int = -1,
            /* optional fields with default values */
            alias: String = ZcashSdk.DEFAULT_ALIAS,
            host: String = ZcashSdk.DEFAULT_LIGHTWALLETD_HOST,
            port: Int = ZcashSdk.DEFAULT_LIGHTWALLETD_PORT
        ) : this(viewingKeys, null, birthdayHeight, alias, host, port) {
            validate()
        }


        fun build(context: Context): Initializer {
            if (birthday == null) {
                birthday = WalletBirthdayTool.loadNearest(context, birthdayHeight)
            }
            return Initializer(context, this)
        }

        /**
         * Add viewing keys to the set of accounts to monitor. Note: Using more than one viewing key
         * is not currently well supported. Consider it an alpha-preview feature that might work but
         * probably has serious bugs.
         */
        fun setViewingKeys(vararg extendedFullViewingKeys: String) {
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
        fun addViewingKey(extendedFullViewingKey: String) {
            viewingKeys.add(extendedFullViewingKey)
        }

        /**
         * Load the most recent checkpoint available. This is useful for new wallets.
         */
        fun newWalletBirthday() {
            birthdayHeight = null
        }

        /**
         * Load the birthday checkpoint closest to the given wallet birthday. This is useful when
         * importing a pre-existing wallet. It is the same as calling
         * `birthdayHeight = importedHeight`.
         */
        fun importedWalletBirthday(importedHeight: Int) {
            birthdayHeight = importedHeight
        }

        /**
         * Theoretically, the oldest possible birthday a wallet could have. Useful for searching
         * all transactions on the chain. In reality, no wallets were born at this height.
         */
        fun saplingBirthday() {
            birthdayHeight = ZcashSdk.SAPLING_ACTIVATION_HEIGHT
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
            // one of the fields must be properly set
            require((birthdayHeight ?: -1) >= ZcashSdk.SAPLING_ACTIVATION_HEIGHT
                    || birthdayHeight != null) {
                "Birthday is required but was not set on this initializer. Verify that a valid" +
                        " birthday was provided when creating the Initializer such as" +
                        " WalletBirthdayTool.loadNearest()"
            }

            // but not both
            require((birthdayHeight ?: -1) < ZcashSdk.SAPLING_ACTIVATION_HEIGHT
                    || birthday == null) {
                "Ambiguous birthday. Either the birthday Object or the birthdayHeight Int should" +
                        " be set but not both."
            }

            // the field that is set should contain a proper value
            require(
                (birthdayHeight ?: birthday?.height ?: -1) >= ZcashSdk.SAPLING_ACTIVATION_HEIGHT
            ) {
                "Invalid birthday height of ${birthdayHeight ?: birthday?.height}. The birthday" +
                        " height must be at least the height of Sapling activation on" +
                        " ${ZcashSdk.NETWORK} (${ZcashSdk.SAPLING_ACTIVATION_HEIGHT})."
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
