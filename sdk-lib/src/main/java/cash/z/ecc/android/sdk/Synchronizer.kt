package cash.z.ecc.android.sdk

import android.content.Context
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.Derivation
import cash.z.ecc.android.sdk.internal.SaplingParamTool
import cash.z.ecc.android.sdk.internal.db.DatabaseCoordinator
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.PercentDecimal
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.TransactionRecipient
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.CheckpointTool
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.AddressType
import cash.z.ecc.android.sdk.type.ConsensusMatchType
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import java.io.Closeable

@Suppress("TooManyFunctions")
interface Synchronizer {

    // Status

    /**
     * The network to which this synchronizer is connected and from which it is processing blocks.
     */
    val network: ZcashNetwork

    /**
     * A flow of values representing the [Status] of this Synchronizer. As the status changes, a new
     * value will be emitted by this flow.
     */
    val status: Flow<Status>

    /**
     * A flow of progress values, typically corresponding to this Synchronizer downloading blocks.
     * Typically, any non-zero value below `PercentDecimal.ONE_HUNDRED_PERCENT` indicates that progress indicators can
     * be shown and a value of `PercentDecimal.ONE_HUNDRED_PERCENT` signals that progress is complete and any progress
     * indicators can be hidden.
     */
    val progress: Flow<PercentDecimal>

    /**
     * A flow of processor details, updated every time blocks are processed to include the latest
     * block height, blocks downloaded and blocks scanned. Similar to the [progress] flow but with a
     * lot more detail.
     */
    val processorInfo: Flow<CompactBlockProcessor.ProcessorInfo>

    /**
     * The latest height observed on the network, which does not necessarily correspond to the
     * latest downloaded height or scanned height. Although this is present in [processorInfo], it
     * is such a frequently used value that it is convenient to have the real-time value by itself.
     */
    val networkHeight: StateFlow<BlockHeight?>

    /**
     * A stream of balance values for the orchard pool. Includes the available and total balance.
     */
    val orchardBalances: StateFlow<WalletBalance?>

    /**
     * A stream of balance values for the sapling pool. Includes the available and total balance.
     */
    val saplingBalances: StateFlow<WalletBalance?>

    /**
     * A stream of balance values for the transparent pool. Includes the available and total balance.
     */
    val transparentBalances: StateFlow<WalletBalance?>

    /**
     * A flow of all the transactions that are on the blockchain.
     */
    val transactions: Flow<List<TransactionOverview>>

    //
    // Latest Properties
    //

    /**
     * An in-memory reference to the latest height seen on the network.
     */
    val latestHeight: BlockHeight?

    /**
     * An in-memory reference to the best known birthday height, which can change if the first
     * transaction has not yet occurred.
     */
    val latestBirthdayHeight: BlockHeight?

    //
    // Operations
    //

    /**
     * Adds the next available account-level spend authority, given the current set of
     * [ZIP 316](https://zips.z.cash/zip-0316) account identifiers known, to the wallet
     * database.
     *
     * The caller should store the byte encoding of the returned spending key in a secure
     * fashion. This encoding **MUST NOT** be exposed to users. It is an internal encoding
     * that is inherently unstable, and only intended to be passed between the SDK and the
     * storage backend. The caller **MUST NOT** allow this encoding to be exported or
     * imported.
     *
     * If `seed` was imported from a backup and this method is being used to restore a
     * previous wallet state, you should use this method to add all of the desired
     * accounts before scanning the chain from the seed's birthday height.
     *
     * By convention, wallets should only allow a new account to be generated after funds
     * have been received by the currently-available account (in order to enable
     * automated account recovery).
     *
     * @param seed the wallet's seed phrase.
     *
     * @return the newly created ZIP 316 account identifier, along with the binary
     * encoding of the `UnifiedSpendingKey` for the newly created account.
     */
    // This is not yet ready to be a public API
    // suspend fun createAccount(seed: ByteArray): UnifiedSpendingKey

    /**
     * Gets the current unified address for the given account.
     *
     * @param account the account whose address is of interest. Use Account.DEFAULT to get a result for the first
     * account.
     *
     * @return the current unified address for the given account.
     */
    suspend fun getUnifiedAddress(account: Account): String

    /**
     * Gets the legacy Sapling address corresponding to the current unified address for the given account.
     *
     * @param account the account whose address is of interest. Use Account.DEFAULT to get a result for the first
     * account.
     *
     * @return a legacy Sapling address for the given account.
     */
    suspend fun getSaplingAddress(account: Account): String

    /**
     * Gets the legacy transparent address corresponding to the current unified address for the given account.
     *
     * @param account the account whose address is of interest. Use Account.DEFAULT to get a result for the first
     * account.
     *
     * @return a legacy transparent address for the given account.
     */
    suspend fun getTransparentAddress(account: Account): String

    /**
     * Sends zatoshi.
     *
     * @param usk the unified spending key associated with the notes that will be spent.
     * @param zatoshi the amount of zatoshi to send.
     * @param toAddress the recipient's address.
     * @param memo the optional memo to include as part of the transaction.
     *
     * @return a flow of PendingTransaction objects representing changes to the state of the
     * transaction. Any time the state changes a new instance will be emitted by this flow. This is
     * useful for updating the UI without needing to poll. Of course, polling is always an option
     * for any wallet that wants to ignore this return value.
     */
    suspend fun sendToAddress(
        usk: UnifiedSpendingKey,
        amount: Zatoshi,
        toAddress: String,
        memo: String = ""
    ): Long

    suspend fun shieldFunds(
        usk: UnifiedSpendingKey,
        memo: String = ZcashSdk.DEFAULT_SHIELD_FUNDS_MEMO_PREFIX
    ): Long

    /**
     * Returns true when the given address is a valid z-addr. Invalid addresses will throw an
     * exception. See valid z-addresses characteristics in related ZIP.
     *
     * @param address the address to validate.
     *
     * @return true when the given address is a valid z-addr.
     *
     * @throws RuntimeException when the address is invalid.
     */
    suspend fun isValidShieldedAddr(address: String): Boolean

    /**
     * Returns true when the given address is a valid t-addr. Invalid addresses will throw an
     * exception. See valid t-addresses characteristics in related ZIP.
     *
     * @param address the address to validate.
     *
     * @return true when the given address is a valid t-addr.
     *
     * @throws RuntimeException when the address is invalid.
     */
    suspend fun isValidTransparentAddr(address: String): Boolean

    /**
     * Returns true when the given address is a valid ZIP 316 unified address.
     *
     * This method is intended for type checking (e.g. form validation). Invalid
     * addresses will throw an exception.
     *
     * @param address the address to validate.
     *
     * @return true when the given address is a valid unified address.
     *
     * @throws RuntimeException when the address is invalid.
     */
    suspend fun isValidUnifiedAddr(address: String): Boolean

    /**
     * Validate whether the server and this SDK share the same consensus branch. This is
     * particularly important to check around network updates so that any wallet that's connected to
     * an incompatible server can surface that information effectively. For the SDK, the consensus
     * branch is used when creating transactions as each one needs to target a specific branch. This
     * function compares the server's branch id to this SDK's and returns information that helps
     * determine whether they match.
     *
     * @return an instance of [ConsensusMatchType] that is essentially a wrapper for both branch ids
     * and provides helper functions for communicating detailed errors to the user.
     */
    suspend fun validateConsensusBranch(): ConsensusMatchType

    /**
     * Validates the given address, returning information about why it is invalid. This is a
     * convenience method that combines the behavior of [isValidShieldedAddr],
     * [isValidTransparentAddr], and [isValidUnifiedAddr] into one call so that the developer
     * doesn't have to worry about handling the exceptions that they throw. Rather, exceptions
     * are converted to [AddressType.Invalid] which has a `reason` property describing why it is
     * invalid.
     *
     * @param address the address to validate.
     *
     * @return an instance of [AddressType] providing validation info regarding the given address.
     */
    suspend fun validateAddress(address: String): AddressType

    /**
     * Download all UTXOs for the given account addresses and store any new ones in the database.
     *
     * @param account The Account, for which all addresses blocks will be downloaded. Use Account.DEFAULT to get a
     * result for the first account.
     * @param since The BlockHeight, from which blocks will be downloaded.
     *
     * @return the number of utxos that were downloaded and added to the UTXO table.
     */
    suspend fun refreshUtxos(
        account: Account,
        since: BlockHeight = network.saplingActivationHeight
    ): Int?

    /**
     * Returns the balance that the wallet knows about. This should be called after [refreshUtxos].
     */
    suspend fun getTransparentBalance(tAddr: String): WalletBalance

    /**
     * Returns the safest height to which we can rewind, given a desire to rewind to the height
     * provided. Due to how witness incrementing works, a wallet cannot simply rewind to any
     * arbitrary height. This handles all that complexity yet remains flexible in the future as
     * improvements are made.
     */
    suspend fun getNearestRewindHeight(height: BlockHeight): BlockHeight

    /**
     * Rewinds to the safest height to which we can rewind, given a desire to rewind to the height
     * provided. Due to how witness incrementing works, a wallet cannot simply rewind to any
     * arbitrary height. This handles all that complexity yet remains flexible in the future as
     * improvements are made.
     */
    suspend fun rewindToNearestHeight(height: BlockHeight)

    /**
     * Rewinds to the safest height approximately 14 days backward from the current chain tip. Due to how witness
     * incrementing works, a wallet cannot simply rewind to any arbitrary height. This handles all that complexity
     * yet remains flexible in the future as improvements are made.
     */
    suspend fun quickRewind()

    /**
     * Returns a stream of memos for a transaction. It works for both received and sent transaction.
     *
     * Note that this function internally resolves any error which comes, logs it, and then transforms it to an empty
     * string.
     *
     * @param transactionOverview For which the memos will be queried
     * @return Flow of memo strings
     */
    fun getMemos(transactionOverview: TransactionOverview): Flow<String>

    /**
     * Returns a list of recipients for a transaction.
     */
    fun getRecipients(transactionOverview: TransactionOverview): Flow<TransactionRecipient>

    //
    // Error Handling
    //

    /**
     * Gets or sets a global error handler. This is a useful hook for handling unexpected critical
     * errors.
     *
     * @return true when the error has been handled and the Synchronizer should attempt to continue.
     * False when the error is unrecoverable and the Synchronizer should [stop].
     */
    var onCriticalErrorHandler: ((Throwable?) -> Boolean)?

    /**
     * An error handler for exceptions during processing. For instance, a block might be missing or
     * a reorg may get mishandled or the database may get corrupted.
     *
     * @return true when the error has been handled and the processor should attempt to continue.
     * False when the error is unrecoverable and the processor should [stop].
     */
    var onProcessorErrorHandler: ((Throwable?) -> Boolean)?

    /**
     * An error handler for exceptions while submitting transactions to lightwalletd. For instance,
     * a transaction may get rejected because it would be a double-spend or the user might lose
     * their cellphone signal.
     *
     * @return true when the error has been handled and the sender should attempt to resend. False
     * when the error is unrecoverable and the sender should [stop].
     */
    var onSubmissionErrorHandler: ((Throwable?) -> Boolean)?

    /**
     * Callback for setup errors that occur prior to processing compact blocks. Can be used to
     * override any errors encountered during setup. When this listener is missing then all setup
     * errors will result in the synchronizer not starting. This is particularly useful for wallets
     * to receive a callback right before the SDK will reject a lightwalletd server because it
     * appears not to match.
     *
     * @return true when the setup error should be ignored and processing should be allowed to
     * start. Otherwise, processing will not begin.
     */
    var onSetupErrorHandler: ((Throwable?) -> Boolean)?

    /**
     * A callback to invoke whenever a chain error is encountered. These occur whenever the
     * processor detects a missing or non-chain-sequential block (i.e. a reorg). At a minimum, it is
     * best to log these errors because they are the most common source of bugs and unexpected
     * behavior in wallets, due to the chain data mutating and wallets becoming out of sync.
     */
    var onChainErrorHandler: ((BlockHeight, BlockHeight) -> Any)?

    /**
     * Represents the status of this Synchronizer, which is useful for communicating to the user.
     */
    enum class Status {
        /**
         * Indicates that [stop] has been called on this Synchronizer and it will no longer be used.
         */
        STOPPED,

        /**
         * Indicates that this Synchronizer is disconnected from its lightwalletd server.
         * When set, a UI element may want to turn red.
         */
        DISCONNECTED,

        /**
         * Indicates that the Synchronizer is actively syncing new blocks. It goes through these stages internally:
         * downloading new blocks, validating these blocks, then scanning them, deleting the temporary persisted block
         * files, and enhancing transaction details at the end.
         *
         * In the downloading stage, the Synchronizer is actively downloading new blocks from the
         * server.
         *
         * In the validating stage, the Synchronizer is actively validating new blocks that were downloaded
         * from the server. Blocks need to be verified before they are scanned. This confirms that
         * each block is chain-sequential, thereby detecting missing blocks and reorgs.
         *
         * In the scanning stage, Synchronizer is actively decrypting new blocks that were downloaded
         * from the server.
         *
         * In the deleting stage are all temporary persisted block files removed from the persistence.
         *
         * In the enhancing stage is the Synchronizer actively enhancing newly scanned blocks with additional
         * transaction details, fetched from the server.
         */
        SYNCING,

        /**
         * Indicates that this Synchronizer is fully up to date and ready for all wallet functions.
         * When set, a UI element may want to turn green. In this state, the balance can be trusted.
         */
        SYNCED
    }

    companion object {

        /**
         * Primary method that SDK clients will use to construct a synchronizer.
         *
         * @param zcashNetwork the network to use.
         *
         * @param alias A string used to segregate multiple wallets in the filesystem.  This implies the string
         * should not contain characters unsuitable for the platform's filesystem.  The default value is
         * generally used unless an SDK client needs to support multiple wallets.
         *
         * @param lightWalletEndpoint Server endpoint.  See [cash.z.ecc.android.sdk.model.defaultForNetwork]. If a
         * client wishes to change the server endpoint, the active synchronizer will need to be stopped and a new
         * instance created with a new value.
         *
         * @param seed the wallet's seed phrase. This is required the first time a new wallet is set up. For
         * subsequent calls, seed is only needed if [InitializerException.SeedRequired] is thrown.
         *
         * @param birthday Block height representing the "birthday" of the wallet.  When creating a new wallet, see
         * [BlockHeight.ofLatestCheckpoint].  When restoring an existing wallet, use block height that was first used
         * to create the wallet.  If that value is unknown, null is acceptable but will result in longer
         * sync times.  After sync completes, the birthday can be determined from [Synchronizer.latestBirthdayHeight].
         *
         * @param syncAlgorithm The CompactBlockProcess's type of block syncing algorithm
         *
         * @throws InitializerException.SeedRequired Indicates clients need to call this method again, providing the
         * seed bytes.
         *
         * @throws IllegalStateException If multiple instances of synchronizer with the same network+alias are
         * active at the same time.  Call `close` to finish one synchronizer before starting another one with the same
         * network+alias.
         */
        /*
         * If customized initialization is required (e.g. for dependency injection or testing), see
         * [DefaultSynchronizerFactory].
         */
        @Suppress("LongParameterList", "LongMethod")
        suspend fun new(
            context: Context,
            zcashNetwork: ZcashNetwork,
            alias: String = ZcashSdk.DEFAULT_ALIAS,
            lightWalletEndpoint: LightWalletEndpoint,
            seed: ByteArray?,
            birthday: BlockHeight?,
            syncAlgorithm: CompactBlockProcessor.SyncAlgorithm = CompactBlockProcessor.SyncAlgorithm.LINEAR
        ): CloseableSynchronizer {
            val applicationContext = context.applicationContext

            validateAlias(alias)

            val saplingParamTool = SaplingParamTool.new(applicationContext)

            val loadedCheckpoint = CheckpointTool.loadNearest(
                applicationContext,
                zcashNetwork,
                birthday ?: zcashNetwork.saplingActivationHeight
            )

            val coordinator = DatabaseCoordinator.getInstance(context)
            // The pending transaction database no longer exists, so we can delete the file
            coordinator.deletePendingTransactionDatabase(zcashNetwork, alias)

            val backend = DefaultSynchronizerFactory.defaultBackend(
                zcashNetwork,
                alias,
                saplingParamTool,
                coordinator
            )

            val blockStore =
                DefaultSynchronizerFactory
                    .defaultCompactBlockRepository(coordinator.fsBlockDbRoot(zcashNetwork, alias), backend)

            val repository = DefaultSynchronizerFactory.defaultDerivedDataRepository(
                applicationContext,
                backend,
                coordinator.dataDbFile(zcashNetwork, alias),
                zcashNetwork,
                loadedCheckpoint,
                seed,
                Derivation.DEFAULT_NUMBER_OF_ACCOUNTS
            )

            val service = DefaultSynchronizerFactory.defaultService(applicationContext, lightWalletEndpoint)
            val encoder = DefaultSynchronizerFactory.defaultEncoder(backend, saplingParamTool, repository)
            val downloader = DefaultSynchronizerFactory.defaultDownloader(service, blockStore)
            val txManager = DefaultSynchronizerFactory.defaultTxManager(
                encoder,
                service
            )
            val processor = DefaultSynchronizerFactory.defaultProcessor(
                backend = backend,
                downloader = downloader,
                repository = repository,
                birthdayHeight = birthday ?: zcashNetwork.saplingActivationHeight,
                syncAlgorithm = syncAlgorithm
            )

            return SdkSynchronizer.new(
                zcashNetwork = zcashNetwork,
                alias = alias,
                repository = repository,
                txManager = txManager,
                processor = processor,
                backend = backend
            )
        }

        /**
         * Effectively the same as [new] although designed to be a blocking call with better
         * interoperability with Java clients.
         *
         * This is a blocking call, so it should not be called from the main thread.
         */
        @JvmStatic
        @Suppress("LongParameterList")
        fun newBlocking(
            context: Context,
            zcashNetwork: ZcashNetwork,
            alias: String = ZcashSdk.DEFAULT_ALIAS,
            lightWalletEndpoint: LightWalletEndpoint,
            seed: ByteArray?,
            birthday: BlockHeight?,
            syncAlgorithm: CompactBlockProcessor.SyncAlgorithm = CompactBlockProcessor.SyncAlgorithm.LINEAR
        ): CloseableSynchronizer = runBlocking {
            new(context, zcashNetwork, alias, lightWalletEndpoint, seed, birthday, syncAlgorithm)
        }

        /**
         * Delete the databases associated with this wallet. This removes all compact blocks and
         * data derived from those blocks. Although most data can be regenerated by setting up a new
         * Synchronizer instance with the seed, there are two special cases where data is not retained:
         * 1. Outputs created with a `null` OVK
         * 2. The UA to which a transaction was sent (recovery from seed will only reveal the receiver, not the full UA)
         *
         * @param appContext the application context.
         * @param network the network associated with the data to be erased.
         * @param alias the alias used to create the local data.
         *
         * @return true when one of the associated files was found. False most likely indicates
         * that the wrong alias was provided.
         */
        suspend fun erase(
            appContext: Context,
            network: ZcashNetwork,
            alias: String = ZcashSdk.DEFAULT_ALIAS
        ): Boolean = SdkSynchronizer.erase(appContext, network, alias)
    }
}

interface CloseableSynchronizer : Synchronizer, Closeable

/**
 * Validate that the alias doesn't contain malicious characters by enforcing simple rules which
 * permit the alias to be used as part of a file name for the preferences and databases. This
 * enables multiple wallets to exist on one device, which is also helpful for sweeping funds.
 *
 * @param alias the alias to validate.
 *
 * @throws IllegalArgumentException whenever the alias is not less than 100 characters or
 * contains something other than alphanumeric characters. Underscores and hyphens are allowed.
 */
private fun validateAlias(alias: String) {
    require(
        alias.length in ZcashSdk.ALIAS_MIN_LENGTH..ZcashSdk.ALIAS_MAX_LENGTH &&
            alias.all { it.isLetterOrDigit() || it == '_' || it == '-' }
    ) {
        "ERROR: Invalid alias ($alias). For security, the alias must be shorter than 100 " +
            "characters and only contain letters, digits, hyphens, and underscores."
    }
}
