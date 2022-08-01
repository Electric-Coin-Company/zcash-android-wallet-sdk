package cash.z.ecc.android.sdk

import android.content.Context
import cash.z.ecc.android.sdk.db.DatabaseCoordinator
import cash.z.ecc.android.sdk.exception.InitializerException
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.ext.getCacheDirSuspend
import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.tool.CheckpointTool
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.UnifiedViewingKey
import cash.z.ecc.android.sdk.type.ZcashNetwork
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Simplified Initializer focused on starting from a ViewingKey.
 */
class Initializer private constructor(
    val context: Context,
    internal val rustBackend: RustBackend,
    val network: ZcashNetwork,
    val alias: String,
    val host: String,
    val port: Int,
    val viewingKeys: List<UnifiedViewingKey>,
    val overwriteVks: Boolean,
    internal val checkpoint: Checkpoint
) {

    suspend fun erase() = erase(context, network, alias)

    class Config private constructor(
        val viewingKeys: MutableList<UnifiedViewingKey> = mutableListOf(),
        var alias: String = ZcashSdk.DEFAULT_ALIAS
    ) {
        var birthdayHeight: BlockHeight? = null
            private set

        lateinit var network: ZcashNetwork
            private set

        lateinit var host: String
            private set

        var port: Int = ZcashNetwork.Mainnet.defaultPort
            private set

        /**
         * Determines the default behavior for null birthdays. When null, nothing has been specified
         * so a null birthdayHeight value is an error. When false, null birthdays will be replaced
         * with the most recent checkpoint height available (typically, the latest `*.json` file in
         * `assets/co.electriccoin.zcash/checkpoint/`). When true, null birthdays will be replaced with the oldest
         * reasonable height where a transaction could exist (typically, sapling activation but
         * better approximations could be devised in the future, such as the date when the first
         * BIP-39 zcash wallets came online).
         */
        var defaultToOldestHeight: Boolean? = null
            private set

        var overwriteVks: Boolean = false
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
         * transactions. Again, this value is only considered when [height] is null.
         *
         */
        fun setBirthdayHeight(height: BlockHeight?, defaultToOldestHeight: Boolean): Config =
            apply {
                this.birthdayHeight = height
                this.defaultToOldestHeight = defaultToOldestHeight
            }

        /**
         * Load the most recent checkpoint available. This is useful for new wallets.
         */
        fun newWalletBirthday(): Config = apply {
            birthdayHeight = null
            defaultToOldestHeight = false
        }

        /**
         * Load the birthday checkpoint closest to the given wallet birthday. This is useful when
         * importing a pre-existing wallet. It is the same as calling
         * `birthdayHeight = importedHeight`.
         */
        fun importedWalletBirthday(importedHeight: BlockHeight?): Config = apply {
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
        fun setViewingKeys(
            vararg unifiedViewingKeys: UnifiedViewingKey,
            overwrite: Boolean = false
        ): Config = apply {
            overwriteVks = overwrite
            viewingKeys.apply {
                clear()
                addAll(unifiedViewingKeys)
            }
        }

        fun setOverwriteKeys(isOverwrite: Boolean) {
            overwriteVks = isOverwrite
        }

        /**
         * Add viewing key to the set of accounts to monitor. Note: Using more than one viewing key
         * is not currently well supported. Consider it an alpha-preview feature that might work but
         * probably has serious bugs.
         */
        fun addViewingKey(unifiedFullViewingKey: UnifiedViewingKey): Config = apply {
            viewingKeys.add(unifiedFullViewingKey)
        }

        //
        // Convenience functions
        //

        /**
         * Set the server and the network property at the same time to prevent them from getting out
         * of sync. Ultimately, this determines which host a synchronizer will use in order to
         * connect to lightwalletd. In most cases, the default host is sufficient but an override
         * can be provided. The host cannot be changed without explicitly setting the network.
         *
         * @param network the Zcash network to use. Either testnet or mainnet.
         * @param host the lightwalletd host to use.
         * @param port the lightwalletd port to use.
         */
        fun setNetwork(
            network: ZcashNetwork,
            host: String = network.defaultHost,
            port: Int = network.defaultPort
        ): Config = apply {
            this.network = network
            this.host = host
            this.port = port
        }

        /**
         * Import a wallet using the first viewing key derived from the given seed.
         */
        suspend fun importWallet(
            seed: ByteArray,
            birthday: BlockHeight?,
            network: ZcashNetwork,
            host: String = network.defaultHost,
            port: Int = network.defaultPort,
            alias: String = ZcashSdk.DEFAULT_ALIAS
        ): Config =
            importWallet(
                DerivationTool.deriveUnifiedViewingKeys(seed, network = network)[0],
                birthday,
                network,
                host,
                port,
                alias
            )

        /**
         * Default function for importing a wallet.
         */
        fun importWallet(
            viewingKey: UnifiedViewingKey,
            birthday: BlockHeight?,
            network: ZcashNetwork,
            host: String = network.defaultHost,
            port: Int = network.defaultPort,
            alias: String = ZcashSdk.DEFAULT_ALIAS
        ): Config = apply {
            setViewingKeys(viewingKey)
            setNetwork(network, host, port)
            importedWalletBirthday(birthday)
            this.alias = alias
        }

        /**
         * Create a new wallet using the first viewing key derived from the given seed.
         */
        suspend fun newWallet(
            seed: ByteArray,
            network: ZcashNetwork,
            host: String = network.defaultHost,
            port: Int = network.defaultPort,
            alias: String = ZcashSdk.DEFAULT_ALIAS
        ): Config = newWallet(
            DerivationTool.deriveUnifiedViewingKeys(seed, network)[0],
            network,
            host,
            port,
            alias
        )

        /**
         * Default function for creating a new wallet.
         */
        fun newWallet(
            viewingKey: UnifiedViewingKey,
            network: ZcashNetwork,
            host: String = network.defaultHost,
            port: Int = network.defaultPort,
            alias: String = ZcashSdk.DEFAULT_ALIAS
        ): Config = apply {
            setViewingKeys(viewingKey)
            setNetwork(network, host, port)
            newWalletBirthday()
            this.alias = alias
        }

        /**
         * Convenience method for setting thew viewingKeys from a given seed. This is the same as
         * calling `setViewingKeys` with the keys that match this seed.
         */
        suspend fun setSeed(
            seed: ByteArray,
            network: ZcashNetwork,
            numberOfAccounts: Int = 1
        ): Config =
            apply {
                setViewingKeys(
                    *DerivationTool.deriveUnifiedViewingKeys(
                        seed,
                        network,
                        numberOfAccounts
                    )
                )
            }

        /**
         * Sets the network from a network id, throwing an exception if the id is not recognized.
         *
         * @param networkId the ID of the network corresponding to the [ZcashNetwork] enum.
         * Typically, it is 0 for testnet and 1 for mainnet.
         */
        fun setNetworkId(networkId: Int): Config = apply {
            network = ZcashNetwork.from(networkId)
        }

        //
        // Validation helpers
        //

        fun validate(): Config = apply {
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
                (birthdayHeight?.value ?: network.saplingActivationHeight.value)
                < network.saplingActivationHeight.value
            ) {
                throw InitializerException.InvalidBirthdayHeightException(birthdayHeight, network)
            }
        }

        private fun validateViewingKeys() {
            require(viewingKeys.isNotEmpty()) {
                "Unified Viewing keys are required. Ensure that the unified viewing keys or seed" +
                    " have been set on this Initializer."
            }
            viewingKeys.forEach {
                DerivationTool.validateUnifiedViewingKey(it)
            }
        }

        companion object
    }

    companion object : SdkSynchronizer.Erasable {

        suspend fun new(appContext: Context, config: Config) = new(appContext, null, config)

        fun newBlocking(appContext: Context, config: Config) = runBlocking {
            new(
                appContext,
                null,
                config
            )
        }

        suspend fun new(
            appContext: Context,
            onCriticalErrorHandler: ((Throwable?) -> Boolean)? = null,
            block: (Config) -> Unit
        ) = new(appContext, onCriticalErrorHandler, Config(block))

        suspend fun new(
            context: Context,
            onCriticalErrorHandler: ((Throwable?) -> Boolean)?,
            config: Config
        ): Initializer {
            config.validate()

            val loadedCheckpoint = run {
                val height = config.birthdayHeight
                    ?: if (config.defaultToOldestHeight == true) {
                        config.network.saplingActivationHeight
                    } else {
                        null
                    }

                CheckpointTool.loadNearest(
                    context,
                    config.network,
                    height
                )
            }

            val rustBackend = initRustBackend(context, config.network, config.alias, loadedCheckpoint.height)

            return Initializer(
                context.applicationContext,
                rustBackend,
                config.network,
                config.alias,
                config.host,
                config.port,
                config.viewingKeys,
                config.overwriteVks,
                loadedCheckpoint
            )
        }

        private fun onCriticalError(onCriticalErrorHandler: ((Throwable?) -> Boolean)?, error: Throwable) {
            twig("********")
            twig("********  INITIALIZER ERROR: $error")
            if (error.cause != null) twig("******** caused by ${error.cause}")
            if (error.cause?.cause != null) twig("******** caused by ${error.cause?.cause}")
            twig("********")
            twig(error)

            if (onCriticalErrorHandler == null) {
                twig(
                    "WARNING: a critical error occurred on the Initializer but no callback is " +
                        "registered to be notified of critical errors! THIS IS PROBABLY A MISTAKE. To " +
                        "respond to these errors (perhaps to update the UI or alert the user) set " +
                        "initializer.onCriticalErrorHandler to a non-null value or use the secondary " +
                        "constructor: Initializer(context, handler) { ... }. Note that the synchronizer " +
                        "and initializer BOTH have error handlers and since the initializer exists " +
                        "before the synchronizer, it needs its error handler set separately."
                )
            }

            onCriticalErrorHandler?.invoke(error)
        }

        private suspend fun initRustBackend(
            context: Context,
            network: ZcashNetwork,
            alias: String,
            blockHeight: BlockHeight
        ): RustBackend {
            val coordinator = DatabaseCoordinator.getInstance(context)

            return RustBackend.init(
                coordinator.cacheDbFile(network, alias).absolutePath,
                coordinator.dataDbFile(network, alias).absolutePath,
                File(context.getCacheDirSuspend(), "params").absolutePath,
                network,
                blockHeight
            )
        }

        /**
         * Delete the databases associated with this wallet. This removes all compact blocks and
         * data derived from those blocks. For most wallets, this should not result in a loss of
         * funds because the seed and spending keys are stored separately. This call just removes
         * the associated data but not the seed or spending key, themselves, because those are
         * managed separately by the wallet.
         *
         * @param appContext the application context.
         * @param network the network associated with the data to be erased.
         * @param alias the alias used to create the local data.
         *
         * @return true when one of the associated files was found. False most likely indicates
         * that the wrong alias was provided.
         */
        override suspend fun erase(
            appContext: Context,
            network: ZcashNetwork,
            alias: String
        ): Boolean = DatabaseCoordinator.getInstance(appContext).deleteDatabases(network, alias)
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
    require(
        alias.length in 1..99 && alias[0].isLetter() &&
            alias.all { it.isLetterOrDigit() || it == '_' }
    ) {
        "ERROR: Invalid alias ($alias). For security, the alias must be shorter than 100 " +
            "characters and only contain letters, digits or underscores and start with a letter."
    }
}
