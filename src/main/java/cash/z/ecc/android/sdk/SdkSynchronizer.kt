package cash.z.ecc.android.sdk

import cash.z.ecc.android.sdk.Synchronizer.Status.*
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.block.CompactBlockProcessor.State.*
import cash.z.ecc.android.sdk.block.CompactBlockProcessor.WalletBalance
import cash.z.ecc.android.sdk.db.entity.*
import cash.z.ecc.android.sdk.exception.SynchronizerException
import cash.z.ecc.android.sdk.ext.*
import cash.z.ecc.android.sdk.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.transaction.OutboundTransactionManager
import cash.z.ecc.android.sdk.transaction.PagedTransactionRepository
import cash.z.ecc.android.sdk.transaction.PersistentTransactionManager
import cash.z.ecc.android.sdk.transaction.TransactionRepository
import cash.z.ecc.android.sdk.validate.AddressType
import cash.z.ecc.android.sdk.validate.AddressType.Shielded
import cash.z.ecc.android.sdk.validate.AddressType.Transparent
import cash.z.ecc.android.sdk.validate.ConsensusMatchType
import cash.z.wallet.sdk.rpc.Service
import io.grpc.ManagedChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A Synchronizer that attempts to remain operational, despite any number of errors that can occur.
 * It acts as the glue that ties all the pieces of the SDK together. Each component of the SDK is
 * designed for the potential of stand-alone usage but coordinating all the interactions is non-
 * trivial. So the Synchronizer facilitates this, acting as reference that demonstrates how all the
 * pieces can be tied together. Its goal is to allow a developer to focus on their app rather than
 * the nuances of how Zcash works.
 *
 * @property storage exposes flows of wallet transaction information.
 * @property txManager manages and tracks outbound transactions.
 * @property processor saves the downloaded compact blocks to the cache and then scans those blocks for
 * data related to this wallet.
 */
@ExperimentalCoroutinesApi
class SdkSynchronizer internal constructor(
    private val storage: TransactionRepository,
    private val txManager: OutboundTransactionManager,
    val processor: CompactBlockProcessor
) : Synchronizer {
    private val _balances = ConflatedBroadcastChannel(WalletBalance())
    private val _status = ConflatedBroadcastChannel<Synchronizer.Status>(DISCONNECTED)

    /**
     * The lifespan of this Synchronizer. This scope is initialized once the Synchronizer starts
     * because it will be a child of the parentScope that gets passed into the [start] function.
     * Everything launched by this Synchronizer will be cancelled once the Synchronizer or its
     * parentScope stops. This coordinates with [isStarted] so that it fails early
     * rather than silently, whenever the scope is used before the Synchronizer has been started.
     */
    var coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)
        get() {
            if (!isStarted) {
                throw SynchronizerException.NotYetStarted
            } else {
                return field
            }
        }
        set(value) {
            field = value
            if (value.coroutineContext !is EmptyCoroutineContext) isStarted = true
        }

    /**
     * The channel that this Synchronizer uses to communicate with lightwalletd. In most cases, this
     * should not be needed or used. Instead, APIs should be added to the synchronizer to
     * enable the desired behavior. In the rare case, such as testing, it can be helpful to share
     * the underlying channel to connect to the same service, and use other APIs
     * (such as darksidewalletd) because channels are heavyweight.
     */
    val channel: ManagedChannel get() = (processor.downloader.lightwalletService as LightWalletGrpcService).channel

    var isStarted = false


    //
    // Transactions
    //

    override val balances: Flow<WalletBalance> = _balances.asFlow()
    override val clearedTransactions = storage.allTransactions
    override val pendingTransactions = txManager.getAll()
    override val sentTransactions = storage.sentTransactions
    override val receivedTransactions = storage.receivedTransactions


    //
    // Status
    //

    /**
     * Indicates the status of this Synchronizer. This implementation basically simplifies the
     * status of the processor to focus only on the high level states that matter most. Whenever the
     * processor is finished scanning, the synchronizer updates transaction and balance info and
     * then emits a [SYNCED] status.
     */
    override val status = _status.asFlow()

    /**
     * Indicates the download progress of the Synchronizer. When progress reaches 100, that
     * signals that the Synchronizer is in sync with the network. Balances should be considered
     * inaccurate and outbound transactions should be prevented until this sync is complete. It is
     * a simplified version of [processorInfo].
     */
    override val progress: Flow<Int> = processor.progress

    /**
     * Indicates the latest information about the blocks that have been processed by the SDK. This
     * is very helpful for conveying detailed progress and status to the user.
     */
    override val processorInfo: Flow<CompactBlockProcessor.ProcessorInfo> = processor.processorInfo


    //
    // Error Handling
    //

    /**
     * A callback to invoke whenever an uncaught error is encountered. By definition, the return
     * value of the function is ignored because this error is unrecoverable. The only reason the
     * function has a return value is so that all error handlers work with the same signature which
     * allows one function to handle all errors in simple apps. This callback is not called on the
     * main thread so any UI work would need to switch context to the main thread.
     */
    override var onCriticalErrorHandler: ((Throwable?) -> Boolean)? = null

    /**
     * A callback to invoke whenever a processor error is encountered. Returning true signals that
     * the error was handled and a retry attempt should be made, if possible. This callback is not
     * called on the main thread so any UI work would need to switch context to the main thread.
     */
    override var onProcessorErrorHandler: ((Throwable?) -> Boolean)? = null

    /**
     * A callback to invoke whenever a server error is encountered while submitting a transaction to
     * lightwalletd. Returning true signals that the error was handled and a retry attempt should be
     * made, if possible. This callback is not called on the main thread so any UI work would need
     * to switch context to the main thread.
     */
    override var onSubmissionErrorHandler: ((Throwable?) -> Boolean)? = null

    /**
     * A callback to invoke whenever a chain error is encountered. These occur whenever the
     * processor detects a missing or non-chain-sequential block (i.e. a reorg).
     */
    override var onChainErrorHandler: ((errorHeight: Int, rewindHeight: Int) -> Any)? = null


    //
    // Public API
    //

    /**
     * Convenience function for the latest balance. Instead of using this, a wallet will more likely
     * want to consume the flow of balances using [balances].
     */
    override val latestBalance: WalletBalance get() = _balances.value

    /**
     * Convenience function for the latest height. Specifically, this value represents the last
     * height that the synchronizer has observed from the lightwalletd server. Instead of using
     * this, a wallet will more likely want to consume the flow of processor info using
     * [processorInfo].
     */
    override val latestHeight: Int get() = processor.currentInfo.networkBlockHeight

    /**
     * Starts this synchronizer within the given scope. For simplicity, attempting to start an
     * instance that has already been started will throw a [SynchronizerException.FalseStart]
     * exception. This reduces the complexity of managing resources that must be recycled. Instead,
     * each synchronizer is designed to have a long lifespan and should be started from an activity,
     * application or session.
     *
     * @param parentScope the scope to use for this synchronizer, typically something with a
     * lifecycle such as an Activity for single-activity apps or a logged in user session. This
     * scope is only used for launching this synchronizer's job as a child. If no scope is provided,
     * then this synchronizer and all of its coroutines will run until stop is called, which is not
     * recommended since it can leak resources. That type of behavior is more useful for tests.
     *
     * @return an instance of this class so that this function can be used fluidly.
     */
    override fun start(parentScope: CoroutineScope?): Synchronizer {
        if (isStarted) throw SynchronizerException.FalseStart
        // base this scope on the parent so that when the parent's job cancels, everything here
        // cancels as well also use a supervisor job so that one failure doesn't bring down the
        // whole synchronizer
        val supervisorJob = SupervisorJob(parentScope?.coroutineContext?.get(Job))
        CoroutineScope(supervisorJob + Dispatchers.Main).let { scope ->
            coroutineScope = scope
            scope.onReady()
        }
        return this
    }

    /**
     * Stop this synchronizer and all of its child jobs. Once a synchronizer has been stopped it
     * should not be restarted and attempting to do so will result in an error. Also, this function
     * will throw an exception if the synchronizer was never previously started.
     */
    override fun stop() {
        coroutineScope.launch {
            processor.stop()
            coroutineScope.cancel()
            _balances.cancel()
            _status.cancel()
        }
    }

    /**
     * Convenience function that exposes the underlying server information, like its name and
     * consensus branch id. Most wallets should already have a different source of truth for the
     * server(s) with which they operate.
     */
    override suspend fun getServerInfo(): Service.LightdInfo = processor.downloader.getServerInfo()

    
    //
    // Storage APIs
    //

    // TODO: turn this section into the data access API. For now, just aggregate all the things that we want to do with the underlying data

    fun findBlockHash(height: Int): ByteArray? {
        return (storage as? PagedTransactionRepository)?.findBlockHash(height)
    }

    fun findBlockHashAsHex(height: Int): String? {
        return findBlockHash(height)?.toHexReversed()
    }

    fun getTransactionCount(): Int {
        return (storage as? PagedTransactionRepository)?.getTransactionCount() ?: 0
    }


    //
    // Private API
    //

    private fun refreshTransactions() {
        storage.invalidate()
    }

    /**
     * Calculate the latest balance, based on the blocks that have been scanned and transmit this
     * information into the flow of [balances].
     */
    suspend fun refreshBalance() {
        twig("refreshing balance")
        _balances.send(processor.getBalanceInfo())
    }

    private fun CoroutineScope.onReady() = launch(CoroutineExceptionHandler(::onCriticalError)) {
        twig("Synchronizer (${this@SdkSynchronizer}) Ready. Starting processor!")
        processor.onProcessorErrorListener = ::onProcessorError
        processor.onChainErrorListener = ::onChainError
        processor.state.onEach {
            when (it) {
                is Scanned -> {
                    // do a bit of housekeeping and then report synced status
                    onScanComplete(it.scannedRange)
                    SYNCED
                }
                is Stopped -> STOPPED
                is Disconnected -> DISCONNECTED
                is Downloading, Initialized -> DOWNLOADING
                is Validating -> VALIDATING
                is Scanning -> SCANNING
                is Enhancing -> ENHANCING
            }.let { synchronizerStatus ->
                //  ignore enhancing status for now
                // TODO: clean this up and handle enhancing gracefully
                if (synchronizerStatus != ENHANCING) _status.send(synchronizerStatus)
            }
        }.launchIn(this)
        processor.start()
        twig("Synchronizer onReady complete. Processor start has exited!")
    }

    private fun onCriticalError(unused: CoroutineContext, error: Throwable) {
        twig("********")
        twig("********  ERROR: $error")
        if (error.cause != null) twig("******** caused by ${error.cause}")
        if (error.cause?.cause != null) twig("******** caused by ${error.cause?.cause}")
        twig("********")

        onCriticalErrorHandler?.invoke(error)
    }

    private fun onFailedSend(error: Throwable): Boolean {
        twig("ERROR while submitting transaction: $error")
        return onSubmissionErrorHandler?.invoke(error)?.also {
            if (it) twig("submission error handler signaled that we should try again!")
        } == true
    }

    private fun onProcessorError(error: Throwable): Boolean {
        twig("ERROR while processing data: $error")
        if (onProcessorErrorHandler == null) {
            twig(
                "WARNING: falling back to the default behavior for processor errors. To add" +
                        " custom behavior, set synchronizer.onProcessorErrorHandler to" +
                        " a non-null value"
            )
            return true
        }
        return onProcessorErrorHandler?.invoke(error)?.also {
            twig(
                "processor error handler signaled that we should " +
                        "${if (it) "try again" else "abort"}!"
            )
        } == true
    }

    private fun onChainError(errorHeight: Int, rewindHeight: Int) {
        twig("Chain error detected at height: $errorHeight. Rewinding to: $rewindHeight")
        if (onChainErrorHandler == null) {
            twig(
                "WARNING: a chain error occurred but no callback is registered to be notified of " +
                "chain errors. To respond to these errors (perhaps to update the UI or alert the" +
                " user) set synchronizer.onChainErrorHandler to a non-null value"
            )
        }
        onChainErrorHandler?.invoke(errorHeight, rewindHeight)
    }

    private suspend fun onScanComplete(scannedRange: IntRange) {
        // TODO: optimize to skip logic here if there are no new transactions with a block height
        //       within the given range

        // TRICKY:
        // Keep an eye on this section because there is a potential for concurrent DB
        // modification. A change in transactions means a change in balance. Calculating the
        // balance requires touching transactions. If both are done in separate threads, the
        // database can have issues. On Android, this would manifest as a false positive for a
        // "malformed database" exception when the database is not actually corrupt but rather
        // locked (i.e. it's a bad error message).
        // The balance refresh is done first because it is coroutine-based and will fully
        // complete by the time the function returns.
        // Ultimately, refreshing the transactions just invalidates views of data that
        // already exists and it completes on another thread so it should come after the
        // balance refresh is complete.
        twigTask("Triggering balance refresh since the processor is synced!") {
            refreshBalance()
        }
        twigTask("Triggering pending transaction refresh!") {
            refreshPendingTransactions()
        }
        twigTask("Triggering transaction refresh since the processor is synced!") {
            refreshTransactions()
        }
    }

    private suspend fun refreshPendingTransactions() = withContext(IO) {
        twig("[cleanup] beginning to refresh and clean up pending transactions")
        // TODO: this would be the place to clear out any stale pending transactions. Remove filter
        //  logic and then delete any pending transaction with sufficient confirmations (all in one
        //  db transaction).
        val allPendingTxs = txManager.getAll().first()
        val lastScannedHeight = storage.lastScannedHeight()

        allPendingTxs.filter { it.isSubmitSuccess() && !it.isMined() }
            .forEach { pendingTx ->
                twig("checking for updates on pendingTx id: ${pendingTx.id}")
                pendingTx.rawTransactionId?.let { rawId ->
                    storage.findMinedHeight(rawId)?.let { minedHeight ->
                        twig(
                            "found matching transaction for pending transaction with id" +
                                    " ${pendingTx.id} mined at height ${minedHeight}!"
                        )
                        txManager.applyMinedHeight(pendingTx, minedHeight)
                    }
                }
            }

        twig("[cleanup] beginning to cleanup cancelled transactions")
        var hasCleaned = false
        // Experimental: cleanup cancelled transactions
        allPendingTxs.filter { it.isCancelled() && it.hasRawTransactionId() }.let { cancellable ->
            cancellable.forEachIndexed { index, pendingTx ->
                twig("[cleanup] FOUND (${index + 1} of ${cancellable.size})" +
                        " CANCELLED pendingTxId: ${pendingTx.id}")
                hasCleaned = hasCleaned || cleanupCancelledTx(pendingTx)
            }
        }

        twig("[cleanup] beginning to cleanup expired transactions")
        // Experimental: cleanup expired transactions
        // note: don't delete the pendingTx until the related data has been scrubbed, or else you
        // lose the thing that identifies the other data as invalid
        // so we first mark the data for deletion, during the previous "cleanup" step, by removing
        // the thing that we're trying to preserve to signal we no longer need it
        // sometimes apps crash or things go wrong and we get an orphaned pendingTx that we'll poll
        // forever, so maybe just get rid of all of them after a long while
        allPendingTxs.filter { (it.isExpired(lastScannedHeight) && it.isMarkedForDeletion())
                || it.isLongExpired(lastScannedHeight) || it.isSafeToDiscard() }
            .forEach {
                val result = txManager.abort(it)
                twig("[cleanup] FOUND EXPIRED pendingTX (lastScanHeight: $lastScannedHeight  expiryHeight: ${it.expiryHeight}): and ${it.id} ${if (result > 0) "successfully removed" else "failed to remove"} it")
            }

        twig("[cleanup] deleting expired transactions from storage")
        hasCleaned = hasCleaned || (storage.deleteExpired(lastScannedHeight) > 0)

        if (hasCleaned) refreshBalance()
        twig("[cleanup] done refreshing and cleaning up pending transactions")
    }

    private suspend fun cleanupCancelledTx(pendingTx: PendingTransaction): Boolean {
        return if (storage.cleanupCancelledTx(pendingTx.rawTransactionId!!)) {
            txManager.markForDeletion(pendingTx.id)
            true
        } else {
            twig("[cleanup] no matching tx was cleaned so the pendingTx will not be marked for deletion")
            false
        }
    }

    //
    // Send / Receive
    //

    override suspend fun cancelSpend(pendingId: Long) = txManager.cancel(pendingId)

    override suspend fun getAddress(accountId: Int): String = processor.getAddress(accountId)

    override fun sendToAddress(
        spendingKey: String,
        zatoshi: Long,
        toAddress: String,
        memo: String,
        fromAccountIndex: Int
    ): Flow<PendingTransaction> = flow {
        twig("Initializing pending transaction")
        // Emit the placeholder transaction, then switch to monitoring the database
        txManager.initSpend(zatoshi, toAddress, memo, fromAccountIndex).let { placeHolderTx ->
            emit(placeHolderTx)
            txManager.encode(spendingKey, placeHolderTx).let { encodedTx ->
                // only submit if it wasn't cancelled. Otherwise cleanup, immediately for best UX.
                if (encodedTx.isCancelled()) {
                    twig("[cleanup] this tx has been cancelled so we will cleanup instead of submitting")
                    if (cleanupCancelledTx(encodedTx)) refreshBalance()
                    encodedTx
                } else {
                    txManager.submit(encodedTx)
                }
            }
        }
    }.flatMapLatest {
        twig("Monitoring pending transaction (id: ${it.id}) for updates...")
        txManager.monitorById(it.id)
    }.distinctUntilChanged()

    override suspend fun isValidShieldedAddr(address: String) =
        txManager.isValidShieldedAddress(address)

    override suspend fun isValidTransparentAddr(address: String) =
        txManager.isValidTransparentAddress(address)

    override suspend fun validateAddress(address: String): AddressType {
        return try {
            if (isValidShieldedAddr(address)) Shielded else Transparent
        } catch (zError: Throwable) {
            var message = zError.message
            try {
                if (isValidTransparentAddr(address)) Transparent else Shielded
            } catch (tError: Throwable) {
                AddressType.Invalid(
                    if (message != tError.message) "$message and ${tError.message}" else (message
                        ?: "Invalid")
                )
            }
        }
    }

    override suspend fun validateConsensusBranch(): ConsensusMatchType {
        val serverBranchId = tryNull { processor.downloader.getServerInfo().consensusBranchId }
        val sdkBranchId = tryNull {
            (txManager as PersistentTransactionManager).encoder.getConsensusBranchId()
        }
        return ConsensusMatchType(
            sdkBranchId?.let { ConsensusBranchId.fromId(it) },
            serverBranchId?.let { ConsensusBranchId.fromHex(it) }
        )
    }
}
