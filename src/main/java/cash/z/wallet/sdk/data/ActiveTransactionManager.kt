package cash.z.wallet.sdk.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Manages active send/receive transactions. These are transactions that have been initiated but not completed with
 * sufficient confirmations. All other transactions are stored in a separate [TransactionRepository].
 */
class ActiveTransactionManager(logger: Twig = SilentTwig()) : CoroutineScope, Twig by logger {

    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.Default + job

    // mutableMapOf gives the same result but we're explicit about preserving insertion order, since we rely on that
    private val activeTransactions = LinkedHashMap<ActiveTransaction, TransactionState>()
    private val channel = ConflatedBroadcastChannel<Map<ActiveTransaction, TransactionState>>()

    fun subscribe(): ReceiveChannel<Map<ActiveTransaction, TransactionState>> {
        return channel.openSubscription()
    }

    fun create(zatoshi: Long, toAddress: String): ActiveSendTransaction {
        return ActiveSendTransaction(zatoshi, toAddress = toAddress).let { setState(it, TransactionState.Creating); it }
    }

    fun failure(transaction: ActiveTransaction, reason: String) {
        setState(transaction, TransactionState.Failure(activeTransactions[transaction], reason))
    }

    fun created(transaction: ActiveSendTransaction, transactionId: Long) {
        setState(transaction, TransactionState.Created(transactionId))
    }

    fun upload(transaction: ActiveSendTransaction) {
        setState(transaction, TransactionState.SendingToNetwork)
    }

    fun awaitConfirmation(transaction: ActiveTransaction, confirmationCount: Int = 0) {
        setState(transaction, TransactionState.AwaitingConfirmations(confirmationCount))
    }

    fun destroy() {
        channel.cancel()
        job.cancel()
    }

    private fun setState(transaction: ActiveTransaction, state: TransactionState) {
        twig("state set to $state for active transaction $transaction on thread ${Thread.currentThread().name}")
        activeTransactions[transaction] = state
        launch {
            channel.send(activeTransactions)
        }
    }
}

data class ActiveSendTransaction(override val value: Long, override val internalId: UUID = UUID.randomUUID(), val toAddress: String) :
    ActiveTransaction

data class ActiveReceiveTransaction(
    val height: Int,
    override val value: Long,
    override val internalId: UUID = UUID.randomUUID()
) :
    ActiveTransaction

interface ActiveTransaction {
    val value: Long
    /** only used by this class for purposes of managing unique transactions */
    val internalId: UUID
}

sealed class TransactionState(val order: Int) {
    object Creating : TransactionState(0) // TODO: ask strad if there is a better name for this scenario

    /** @param txId row in the database where the raw transaction has been stored, temporarily, by the rust lib */
    class Created(val txId: Long) : TransactionState(10)

    object SendingToNetwork : TransactionState(20)

    class AwaitingConfirmations(val confirmationCount: Int) : TransactionState(30)


    /** @param failedStep the state of this transaction at the time, prior to failure */
    class Failure(val failedStep: TransactionState?, val reason: String = "") : TransactionState(40)

    object Success : TransactionState(50)
}