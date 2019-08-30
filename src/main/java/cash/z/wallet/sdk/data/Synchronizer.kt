package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.entity.ClearedTransaction
import cash.z.wallet.sdk.entity.PendingTransaction
import cash.z.wallet.sdk.entity.SentTransaction
import cash.z.wallet.sdk.secure.Wallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Primary interface for interacting with the SDK. Defines the contract that specific implementations like
 * [MockSynchronizer] and [StableSynchronizer] fulfill. Given the language-level support for coroutines, we favor their
 * use in the SDK and incorporate that choice into this contract.
 */
interface Synchronizer {

    //
    // Lifecycle
    //

    /**
     * Starts this synchronizer within the given scope.
     *
     * @param parentScope the scope to use for this synchronizer, typically something with a lifecycle such as an
     * Activity. Implementations should leverage structured concurrency and cancel all jobs when this scope completes.
     */
    fun start(parentScope: CoroutineScope): Synchronizer

    /**
     * Stop this synchronizer. Implementations should ensure that calling this method cancels all jobs that were created
     * by this instance.
     */
    fun stop()


    //
    // Channels
    //

    /**
     * A stream of balance values, separately reflecting both the available and total balance.
     */
    fun balances(): ReceiveChannel<Wallet.WalletBalance>

    /**
     * A stream of progress values, typically corresponding to this Synchronizer downloading blocks. Typically, any non-
     * zero value below 100 indicates that progress indicators can be shown and a value of 100 signals that progress is
     * complete and any progress indicators can be hidden.
     */
    fun progress(): ReceiveChannel<Int>

    /**
     * A stream of all the outbound pending transaction that have been sent but are awaiting confirmations.
     */
    fun pendingTransactions(): ReceiveChannel<List<PendingTransaction>>

    /**
     * A stream of all the transactions that are on the blockchain. Implementations should consider only returning a
     * subset like the most recent 100 transactions, perhaps through paging the underlying database.
     */
    fun clearedTransactions(): ReceiveChannel<List<ClearedTransaction>>

    /**
     * Holds the most recent value that was transmitted through the [pendingTransactions] channel. Typically, if the
     * underlying channel is a BroadcastChannel (and it should be),then this value is simply [pendingChannel.value]
     */
    fun lastPending(): List<PendingTransaction>

    /**
     * Holds the most recent value that was transmitted through the [clearedTransactions] channel. Typically, if the
     * underlying channel is a BroadcastChannel (and it should be), then this value is simply [clearedChannel.value]
     */
    fun lastCleared(): List<ClearedTransaction>

    /**
     * Holds the most recent value that was transmitted through the [balances] channel. Typically, if the
     * underlying channel is a BroadcastChannel (and it should be), then this value is simply [balanceChannel.value]
     */
    fun lastBalance(): Wallet.WalletBalance


    //
    // Status
    //

    /**
     * A flag indicating whether this Synchronizer is connected to its lightwalletd server. When false, a UI element
     * may want to turn red.
     */
    val isConnected: Boolean


    /**
     * A flag indicating whether this Synchronizer is actively downloading compact blocks. When true, a UI element
     * may want to turn yellow.
     */
    val isSyncing: Boolean

    /**
     * A flag indicating whether this Synchronizer is actively decrypting compact blocks, searching for transactions.
     * When true, a UI element may want to turn yellow.
     */
    val isScanning: Boolean


    //
    // Operations
    //

    /**
     * Gets the address for the given account.
     *
     * @param accountId the optional accountId whose address is of interest. By default, the first account is used.
     */
    suspend fun getAddress(accountId: Int = 0): String

    /**
     * Sends zatoshi.
     *
     * @param zatoshi the amount of zatoshi to send.
     * @param toAddress the recipient's address.
     * @param memo the optional memo to include as part of the transaction.
     * @param fromAccountId the optional account id to use. By default, the first account is used.
     */
    suspend fun sendToAddress(
        zatoshi: Long,
        toAddress: String,
        memo: String = "",
        fromAccountId: Int = 0
    ): PendingTransaction

    /**
     * Attempts to cancel a previously sent transaction. Typically, cancellation is only an option if the transaction
     * has not yet been submitted to the server.
     *
     * @param transaction the transaction to cancel.
     * @return true when the cancellation request was successful. False when it is too late to cancel.
     */
    fun cancelSend(transaction: SentTransaction): Boolean


    //
    // Error Handling
    //

    /**
     * Gets or sets a global error handler. This is a useful hook for handling unexpected critical errors.
     *
     * @return true when the error has been handled and the Synchronizer should attempt to continue. False when the
     * error is unrecoverable and the Synchronizer should [stop].
     */
    var onCriticalErrorHandler: ((Throwable?) -> Boolean)?

    /**
     * An error handler for exceptions during processing. For instance, a block might be missing or a reorg may get
     * mishandled or the database may get corrupted.
     *
     * @return true when the error has been handled and the processor should attempt to continue. False when the
     * error is unrecoverable and the processor should [stop].
     */
    var onProcessorErrorHandler: ((Throwable?) -> Boolean)?

    /**
     * An error handler for exceptions while submitting transactions to lightwalletd. For instance, a transaction may
     * get rejected because it would be a double-spend or the user might lose their cellphone signal.
     *
     * @return true when the error has been handled and the sender should attempt to resend. False when the
     * error is unrecoverable and the sender should [stop].
     */
    var onSubmissionErrorHandler: ((Throwable?) -> Boolean)?
}