package cash.z.wallet.sdk

import androidx.paging.PagedList
import cash.z.wallet.sdk.block.CompactBlockProcessor.WalletBalance
import cash.z.wallet.sdk.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

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
     * Starts this synchronizer within the given scope.
     *
     * @param parentScope the scope to use for this synchronizer, typically something with a
     * lifecycle such as an Activity. Implementations should leverage structured concurrency and
     * cancel all jobs when this scope completes.
     */
    fun start(parentScope: CoroutineScope? = null): Synchronizer

    /**
     * Stop this synchronizer. Implementations should ensure that calling this method cancels all
     * jobs that were created by this instance.
     */
    fun stop()


    //
    // Flows
    //

    /* Status */

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
     * A stream of balance values, separately reflecting both the available and total balance.
     */
    val balances: Flow<WalletBalance>

    /* Transactions */

    /**
     * A flow of all the outbound pending transaction that have been sent but are awaiting
     * confirmations.
     */
    val pendingTransactions: Flow<List<PendingTransaction>>

    /**
     * A flow of all the transactions that are on the blockchain.
     */
    val clearedTransactions: Flow<PagedList<ConfirmedTransaction>>

    /**
     * A flow of all transactions related to sending funds.
     */
    val sentTransactions: Flow<PagedList<ConfirmedTransaction>>

    /**
     * A flow of all transactions related to receiving funds.
     */
    val receivedTransactions: Flow<PagedList<ConfirmedTransaction>>


    //
    // Operations
    //

    /**
     * Gets the address for the given account.
     *
     * @param accountId the optional accountId whose address is of interest. By default, the first
     * account is used.
     */
    suspend fun getAddress(accountId: Int = 0): String

    /**
     * Sends zatoshi.
     *
     * @param spendingKey the key that allows spends to occur.
     * @param zatoshi the amount of zatoshi to send.
     * @param toAddress the recipient's address.
     * @param memo the optional memo to include as part of the transaction.
     * @param fromAccountId the optional account id to use. By default, the first account is used.
     */
    fun sendToAddress(
        spendingKey: String,
        zatoshi: Long,
        toAddress: String,
        memo: String = "",
        fromAccountIndex: Int = 0
    ): Flow<PendingTransaction>

    /**
     * Attempts to cancel a transaction that is about to be sent. Typically, cancellation is only
     * an option if the transaction has not yet been submitted to the server.
     *
     * @param transaction the transaction to cancel.
     * @return true when the cancellation request was successful. False when it is too late.
     */
    suspend fun cancelSpend(transaction: PendingTransaction): Boolean


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
         * Indicates that this Synchronizer is not yet synced and therefore should not broadcast
         * transactions because it does not have the latest data. When set, a UI element may want
         * to turn yellow.
         */
        SYNCING,

        /**
         * Indicates that this Synchronizer is fully up to date and ready for all wallet functions.
         * When set, a UI element may want to turn green.
         */
        SYNCED
    }
}