package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.dao.WalletTransaction
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.random.nextLong

/**
 * Utility for building UIs. It does the best it can to mock the synchronizer so that it can be dropped right into any
 * project and drive the UI. It generates active transactions in response to funds being sent and generates random
 * received transactions periodically.
 *
 * @param transactionInterval the time in milliseconds between receive transactions being added because those are the
 * only ones auto-generated. Send transactions are triggered by the UI. Transactions are polled at half this interval.
 * @param activeTransactionUpdateFrequency the amount of time in milliseconds between updates to an active
 * transaction's state. Active transactions move through their lifecycle and increment their state at this rate.
 */
open class MockSynchronizer(
    private val transactionInterval: Long = 30_000L,
    private val activeTransactionUpdateFrequency: Long = 3_000L
) : Synchronizer, CoroutineScope {

    private val mockAddress = "ztestsaplingmock0000this0is0a0mock0address0do0not0send0funds0to0this0address0ok0thanks00"

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    /* only accessed through mutual exclusion */
    private val transactions = mutableListOf<WalletTransaction>()
    private val activeTransactions = mutableMapOf<ActiveTransaction, TransactionState>()

    private val transactionMutex = Mutex()
    private val activeTransactionMutex = Mutex()

    private val forge = Forge()

    private val balanceChannel = ConflatedBroadcastChannel(0L)
    private val activeTransactionsChannel = ConflatedBroadcastChannel<Map<ActiveTransaction, TransactionState>>(mutableMapOf())
    private val transactionsChannel = ConflatedBroadcastChannel<List<WalletTransaction>>(listOf())
    private val progressChannel = ConflatedBroadcastChannel(0)

    override fun start(parentScope: CoroutineScope): Synchronizer {
        forge.start(parentScope)
        return this
    }

    override fun stop() {
        job.cancel()
    }

    override fun activeTransactions() = activeTransactionsChannel.openSubscription()
    override fun allTransactions() = transactionsChannel.openSubscription()
    override fun balance() = balanceChannel.openSubscription()
    override fun progress() = progressChannel.openSubscription()

    override suspend fun isOutOfSync(): Boolean {
        return Random.nextInt(100) < 10
    }

    override suspend fun isFirstRun(): Boolean {
        return Random.nextBoolean()
    }

    override fun getAddress() = mockAddress

    override suspend fun sendToAddress(zatoshi: Long, toAddress: String) = withContext<Unit>(Dispatchers.IO) {
        val walletTransaction = forge.createSendTransaction(zatoshi)
        val activeTransaction = forge.createActiveSendTransaction(walletTransaction, toAddress)
        val isInvalidForTestnet = toAddress.length != 88 && toAddress.startsWith("ztest")
        val isInvalidForMainnet = toAddress.length != 78 && toAddress.startsWith("zs")
        val state = when {
            zatoshi < 0 -> TransactionState.Failure(TransactionState.Creating, "amount cannot be negative")
            !toAddress.startsWith("z") -> TransactionState.Failure(TransactionState.Creating, "address must start with z")
            isInvalidForTestnet -> TransactionState.Failure(TransactionState.Creating, "invalid testnet address")
            isInvalidForMainnet -> TransactionState.Failure(TransactionState.Creating, "invalid mainnet address")
            else -> TransactionState.Creating
        }
        println("after input validation, state is being set to ${state::class.simpleName}")
        setState(activeTransaction, state)

        println("active tx size is ${activeTransactions.size}")

        // next, transition it through the states, if it got created
        if (state !is TransactionState.Creating) {
            println("failed to create transaction")
            return@withContext
        } else {
            // first, add the transaction
            println("adding transaction")
            transactionMutex.withLock {
                transactions.add(walletTransaction)
            }

            // then update the active transaction through the creation and submission steps
            listOf(TransactionState.Created(walletTransaction.txId), TransactionState.SendingToNetwork)
                .forEach { newState ->
                    if (!job.isActive) return@withContext
                    delay(activeTransactionUpdateFrequency)
                    setState(activeTransaction, newState)
                }

            // then set the wallet transaction's height (to simulate it being mined)
            val minedHeight = forge.latestHeight.getAndIncrement()
            transactionMutex.withLock {
                transactions.remove(walletTransaction)
                transactions.add(walletTransaction.copy(height = minedHeight, isMined = true))
            }

            // simply transition it through the states
            List(11) { TransactionState.AwaitingConfirmations(it) }
                .forEach { newState ->
                    if (!job.isActive) return@withContext
                    delay(activeTransactionUpdateFrequency)
                    activeTransaction.height.set(minedHeight + newState.confirmationCount)
                    setState(activeTransaction, newState)
                }
        }
    }

    private suspend fun setState(activeTransaction: ActiveTransaction, state: TransactionState) {
        var copyMap = mutableMapOf<ActiveTransaction, TransactionState>()
        activeTransactionMutex.withLock {
            val currentState = activeTransactions[activeTransaction]
            if ((currentState?.order ?: 0) < 0) {
                println("ignoring state ${state::class.simpleName} because the current state is ${currentState!!::class.simpleName}")
                return
            }
            activeTransactions[activeTransaction] = state
            var count = if (state is TransactionState.AwaitingConfirmations) "(${state.confirmationCount})" else ""
            println("state set to ${state::class.simpleName}$count on thread ${Thread.currentThread().name}")
        }

        copyMap = activeTransactions.toMutableMap()
        println("sending ${copyMap.size} active transactions")
        launch {
            activeTransactionsChannel.send(copyMap)
        }
    }

    override fun cancelSend(transaction: ActiveSendTransaction): Boolean {
        launch {
            println("cancelling transaction $transaction")
            setState(transaction, TransactionState.Cancelled)
        }
        return true
    }


    /* creators */

    private inner class Forge {
        val transactionId = AtomicLong(Random.nextLong(1L..100_000L))
        val latestHeight = AtomicInteger(Random.nextInt(280000..600000))

        fun start(scope: CoroutineScope) {
            scope.launchAddReceiveTransactions()
            scope.launchUpdateTransactionsAndBalance()
            scope.launchUpdateProgress()
        }

        fun CoroutineScope.launchUpdateTransactionsAndBalance() = launch {
            while (job.isActive) {
                if (transactions.size != 0) {
                    var balance = 0L
                    transactionMutex.withLock {
                        // does not factor in confirmations
                        balance =
                                transactions.fold(0L) { acc, tx -> if (tx.isSend && tx.isMined) acc - tx.value else acc + tx.value }
                    }
                    balanceChannel.send(balance)
                }
                // other collaborators add to the list, periodically. So this simulates, real-world, non-distinct updates.
                delay(Random.nextLong(transactionInterval / 2))
                var copyList = listOf<WalletTransaction>()
                transactionMutex.withLock {
                    // shallow copy
                    copyList = transactions.map { it }
                }
                transactionsChannel.send(copyList)
            }
        }

        fun CoroutineScope.launchAddReceiveTransactions() = launch {
            while (job.isActive) {
                delay(transactionInterval)
                transactionMutex.withLock {
                    println("adding received transaction for random value")
                    transactions.add(createReceiveTransaction())
                }
            }
        }

        fun CoroutineScope.launchUpdateProgress() =  launch {
            val progressInterval = Math.max(transactionInterval / 4, 1)
            while (job.isActive) {
                delay(Random.nextLong(1L..progressInterval))
                progressChannel.send(Math.min(transactions.size * 10, 100))
            }
        }

        fun createReceiveTransaction(): WalletTransaction {
            return WalletTransaction(
                txId = transactionId.getAndIncrement(),
                value = Random.nextLong(20_000L..1_000_000_000L),
                height = latestHeight.getAndIncrement(),
                isSend = false,
                timeInSeconds = System.currentTimeMillis() / 1000,
                isMined = true
            )
        }

        fun createSendTransaction(amount: Long = Random.nextLong(20_000L..1_000_000_000L), txId: Long = -1L): WalletTransaction {
            return WalletTransaction(
                txId = if(txId == -1L) transactionId.getAndIncrement() else txId,
                value = amount,
                height = null,
                isSend = true,
                timeInSeconds = System.currentTimeMillis() / 1000,
                isMined = false
            )
        }

        fun createActiveSendTransaction(walletTransaction: WalletTransaction, toAddress: String)
                = createActiveSendTransaction(walletTransaction.value, toAddress, walletTransaction.txId)

        fun createActiveSendTransaction(amount: Long, address: String, txId: Long = -1): ActiveSendTransaction {
            return ActiveSendTransaction(
                transactionId = AtomicLong(if (txId < 0) transactionId.getAndIncrement() else txId),
                toAddress = address,
                value = amount
            )
        }
    }
}