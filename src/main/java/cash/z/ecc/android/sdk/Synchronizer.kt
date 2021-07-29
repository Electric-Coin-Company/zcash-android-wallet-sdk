package cash.z.ecc.android.sdk

import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.type.AddressType
import cash.z.ecc.android.sdk.type.ConsensusMatchType
import cash.z.ecc.android.sdk.type.WalletBalance
import cash.z.ecc.android.sdk.type.ZcashNetwork
import cash.z.wallet.sdk.rpc.Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Primary interface for interacting with the SDK. Defines the contract that specific
 * implementations like [MockSynchronizer] and [SdkSynchronizer] fulfill. Given the language-level
 * support for coroutines, we favor their use in the SDK and incorporate that choice into this
 * contract.
 */
interface Synchronizer {

    //
    // Lifecycle
    //

    /**
     * Return true when this synchronizer has been started.
     */
    var isStarted: Boolean

    /**
     * Prepare the synchronizer to start. Must be called before start. This gives a clear point
     * where setup and maintenance can occur for various Synchronizers. One that uses a database
     * would take this opportunity to do data migrations or key migrations.
     */
    fun prepare(): Synchronizer

    /**
     * Starts this synchronizer within the given scope.
     *
     * @param parentScope the scope to use for this synchronizer, typically something with a
     * lifecycle such as an Activity. Implementations should leverage structured concurrency and
     * cancel all jobs when this scope completes.
     *
     * @return an instance of the class so that this function can be used fluidly.
     */
    fun start(parentScope: CoroutineScope? = null): Synchronizer

    /**
     * Stop this synchronizer. Implementations should ensure that calling this method cancels all
     * jobs that were created by this instance.
     *
     * Note that in most cases, there is no need to call [stop] because the Synchronizer will
     * automatically stop whenever the parentScope is cancelled. For instance, if that scope is
     * bound to the lifecycle of the activity, the Synchronizer will stop when the activity stops.
     * However, if no scope is provided to the start method, then the Synchronizer must be stopped
     * with this function.
     */
    fun stop()

    //
    // Flows
    //

    /* Status */

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
     * Typically, any non- zero value below 100 indicates that progress indicators can be shown and
     * a value of 100 signals that progress is complete and any progress indicators can be hidden.
     */
    val progress: Flow<Int>

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
    val networkHeight: StateFlow<Int>

    /**
     * A stream of balance values for the orchard pool. Includes the available and total balance.
     */
    val orchardBalances: StateFlow<WalletBalance>

    /**
     * A stream of balance values for the sapling pool. Includes the available and total balance.
     */
    val saplingBalances: StateFlow<WalletBalance>

    /**
     * A stream of balance values for the transparent pool. Includes the available and total balance.
     */
    val transparentBalances: StateFlow<WalletBalance>

    /* Transactions */

    /**
     * A flow of all the outbound pending transaction that have been sent but are awaiting
     * confirmations.
     */
    val pendingTransactions: Flow<List<PendingTransaction>>

    /**
     * A flow of all the transactions that are on the blockchain.
     */
    val clearedTransactions: Flow<List<ConfirmedTransaction>>

    /**
     * A flow of all transactions related to sending funds.
     */
    val sentTransactions: Flow<List<ConfirmedTransaction>>

    /**
     * A flow of all transactions related to receiving funds.
     */
    val receivedTransactions: Flow<List<ConfirmedTransaction>>

    //
    // Latest Properties
    //

    /**
     * An in-memory reference to the latest height seen on the network.
     */
    val latestHeight: Int

    /**
     * An in-memory reference to the best known birthday height, which can change if the first
     * transaction has not yet occurred.
     */
    val latestBirthdayHeight: Int

    //
    // Operations
    //

    /**
     * Gets the shielded address for the given account. This is syntactic sugar for
     * [getShieldedAddress] because we use z-addrs by default.
     *
     * @param accountId the optional accountId whose address is of interest. By default, the first
     * account is used.
     *
     * @return the shielded address for the given account.
     */
    suspend fun getAddress(accountId: Int = 0) = getShieldedAddress(accountId)

    /**
     * Gets the shielded address for the given account.
     *
     * @param accountId the optional accountId whose address is of interest. By default, the first
     * account is used.
     *
     * @return the shielded address for the given account.
     */
    suspend fun getShieldedAddress(accountId: Int = 0): String

    /**
     * Gets the transparent address for the given account.
     *
     * @param accountId the optional accountId whose address is of interest. By default, the first
     * account is used.
     *
     * @return the address for the given account.
     */
    suspend fun getTransparentAddress(accountId: Int = 0): String

    /**
     * Sends zatoshi.
     *
     * @param spendingKey the key associated with the notes that will be spent.
     * @param zatoshi the amount of zatoshi to send.
     * @param toAddress the recipient's address.
     * @param memo the optional memo to include as part of the transaction.
     * @param fromAccountIndex the optional account id to use. By default, the first account is used.
     *
     * @return a flow of PendingTransaction objects representing changes to the state of the
     * transaction. Any time the state changes a new instance will be emitted by this flow. This is
     * useful for updating the UI without needing to poll. Of course, polling is always an option
     * for any wallet that wants to ignore this return value.
     */
    fun sendToAddress(
        spendingKey: String,
        zatoshi: Long,
        toAddress: String,
        memo: String = "",
        fromAccountIndex: Int = 0
    ): Flow<PendingTransaction>

    fun shieldFunds(
        spendingKey: String,
        transparentSecretKey: String,
        memo: String = ZcashSdk.DEFAULT_SHIELD_FUNDS_MEMO_PREFIX
    ): Flow<PendingTransaction>

    /**
     * Returns true when the given address is a valid z-addr. Invalid addresses will throw an
     * exception. Valid z-addresses have these characteristics: //TODO copy info from related ZIP
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
     * exception. Valid t-addresses have these characteristics: //TODO copy info from related ZIP
     *
     * @param address the address to validate.
     *
     * @return true when the given address is a valid t-addr.
     *
     * @throws RuntimeException when the address is invalid.
     */
    suspend fun isValidTransparentAddr(address: String): Boolean

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
     * convenience method that combines the behavior of [isValidShieldedAddr] and
     * [isValidTransparentAddr] into one call so that the developer doesn't have to worry about
     * handling the exceptions that they throw. Rather, exceptions are converted to
     * [AddressType.Invalid] which has a `reason` property describing why it is invalid.
     *
     * @param address the address to validate.
     *
     * @return an instance of [AddressType] providing validation info regarding the given address.
     */
    suspend fun validateAddress(address: String): AddressType

    /**
     * Attempts to cancel a transaction that is about to be sent. Typically, cancellation is only
     * an option if the transaction has not yet been submitted to the server.
     *
     * @param pendingId the id of the PendingTransaction to cancel.
     *
     * @return true when the cancellation request was successful. False when it is too late.
     */
    suspend fun cancelSpend(pendingId: Long): Boolean

    /**
     * Convenience function that exposes the underlying server information, like its name and
     * consensus branch id. Most wallets should already have a different source of truth for the
     * server(s) with which they operate and thereby not need this function.
     */
    suspend fun getServerInfo(): Service.LightdInfo

    /**
     * Gracefully change the server that the Synchronizer is currently using. In some cases, this
     * will require waiting until current network activity is complete. Ideally, this would protect
     * against accidentally switching between testnet and mainnet, by comparing the service info of
     * the existing server with that of the new one.
     */
    suspend fun changeServer(
        host: String,
        port: Int = network.defaultPort,
        errorHandler: (Throwable) -> Unit = { throw it }
    )

    /**
     * Download all UTXOs for the given address and store any new ones in the database.
     *
     * @return the number of utxos that were downloaded and addded to the UTXO table.
     */
    suspend fun refreshUtxos(tAddr: String, sinceHeight: Int = network.saplingActivationHeight): Int?

    /**
     * Returns the balance that the wallet knows about. This should be called after [refreshUtxos].
     */
    suspend fun getTransparentBalance(tAddr: String): WalletBalance

    suspend fun getNearestRewindHeight(height: Int): Int

    /**
     * Returns the safest height to which we can rewind, given a desire to rewind to the height
     * provided. Due to how witness incrementing works, a wallet cannot simply rewind to any
     * arbitrary height. This handles all that complexity yet remains flexible in the future as
     * improvements are made.
     */
    suspend fun rewindToNearestHeight(height: Int, alsoClearBlockCache: Boolean = false)

    suspend fun quickRewind()

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
    var onChainErrorHandler: ((Int, Int) -> Any)?

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
         * Indicates that this Synchronizer is actively preparing to start, which usually involves
         * setting up database tables, migrations or taking other maintenance steps that need to
         * occur after an upgrade.
         */
        PREPARING,

        /**
         * Indicates that this Synchronizer is actively downloading new blocks from the server.
         */
        DOWNLOADING,

        /**
         * Indicates that this Synchronizer is actively validating new blocks that were downloaded
         * from the server. Blocks need to be verified before they are scanned. This confirms that
         * each block is chain-sequential, thereby detecting missing blocks and reorgs.
         */
        VALIDATING,

        /**
         * Indicates that this Synchronizer is actively decrypting new blocks that were downloaded
         * from the server.
         */
        SCANNING,

        /**
         * Indicates that this Synchronizer is actively enhancing newly scanned blocks with
         * additional transaction details, fetched from the server.
         */
        ENHANCING,

        /**
         * Indicates that this Synchronizer is fully up to date and ready for all wallet functions.
         * When set, a UI element may want to turn green. In this state, the balance can be trusted.
         */
        SYNCED
    }
}
