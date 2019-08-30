package cash.z.wallet.sdk.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.wallet.sdk.db.PendingTransactionDao
import cash.z.wallet.sdk.db.PendingTransactionDb
import cash.z.wallet.sdk.entity.PendingTransaction
import cash.z.wallet.sdk.entity.Transaction
import cash.z.wallet.sdk.ext.EXPIRY_OFFSET
import cash.z.wallet.sdk.service.LightWalletService
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

/**
 * Facilitates persistent attempts to ensure a transaction occurs.
 */
// TODO: consider having the manager register the fail listeners rather than having that responsibility spread elsewhere (synchronizer and the broom)
class PersistentTransactionManager(private val db: PendingTransactionDb) : TransactionManager {

    private val dao: PendingTransactionDao = db.pendingTransactionDao()

    /**
     * Constructor that creates the database and then executes a callback on it.
     */
    constructor(
        appContext: Context,
        dataDbName: String = "PendingTransactions.db"
    ) : this(
        Room.databaseBuilder(
            appContext,
            PendingTransactionDb::class.java,
            dataDbName
        ).setJournalMode(RoomDatabase.JournalMode.TRUNCATE).build()
    )

    override fun start() {
        twig("TransactionManager starting")
    }

    override fun stop() {
        twig("TransactionManager stopping")
        db.close()
    }

    suspend fun initPlaceholder(
        zatoshiValue: Long,
        toAddress: String,
        memo: String
    ): PendingTransaction? = withContext(IO) {
        twig("constructing a placeholder transaction")
        val tx = initTransaction(zatoshiValue, toAddress, memo)
        twig("done constructing a placeholder transaction")
        try {
            twig("inserting tx into DB: $tx")
            val insertId = dao.insert(tx)
            twig("insert returned id of $insertId")
            tx.copy(id = insertId)
        } catch (t: Throwable) {
            val message = "failed initialize a placeholder transaction due to : ${t.message} caused by: ${t.cause}"
            twig(message)
            null
        } finally {
            twig("done constructing a placeholder transaction")
        }
    }

    override suspend fun manageCreation(
        encoder: TransactionEncoder,
        zatoshiValue: Long,
        toAddress: String,
        memo: String,
        currentHeight: Int
    ): PendingTransaction = manageCreation(encoder, initTransaction(zatoshiValue, toAddress, memo), currentHeight)


    suspend fun manageCreation(
        encoder: TransactionEncoder,
        transaction: PendingTransaction,
        currentHeight: Int
    ): PendingTransaction = withContext(IO){
        twig("managing the creation of a transaction")
        var tx = transaction.copy(expiryHeight = if (currentHeight == -1) -1 else currentHeight + EXPIRY_OFFSET)
        try {
            twig("beginning to encode transaction with : $encoder")
            val encodedTx = encoder.create(tx.value, tx.toAddress, tx.memo ?: "")
            twig("successfully encoded transaction for ${tx.memo}!!")
            tx = tx.copy(raw = encodedTx.raw, rawTransactionId = encodedTx.txId)
            tx
        } catch (t: Throwable) {
            val message = "failed to encode transaction due to : ${t.message} caused by: ${t.cause}"
            twig(message)
            message
            tx = tx.copy(errorMessage = message)
            tx
        } finally {
            tx = tx.copy(encodeAttempts = Math.max(1, tx.encodeAttempts + 1))
            twig("inserting tx into DB: $tx")
            dao.insert(tx)
            twig("successfully inserted TX into DB")
            tx
        }
    }

    override suspend fun manageSubmission(service: LightWalletService, pendingTransaction: SignedTransaction) {
        var tx = pendingTransaction as PendingTransaction
        try {
            twig("managing the preparation to submit transaction memo: ${tx.memo} amount: ${tx.value}")
            val response = service.submitTransaction(pendingTransaction.raw!!)
            twig("management of submit transaction completed with response: ${response.errorCode}: ${response.errorMessage}")
            tx = if (response.errorCode < 0) {
                tx.copy(errorMessage = response.errorMessage, errorCode = response.errorCode)
            } else {
                tx.copy(errorMessage = null, errorCode = response.errorCode)
            }
        } catch (t: Throwable) {
            twig("error while managing submitting transaction: ${t.message} caused by: ${t.cause}")
        } finally {
            tx = tx.copy(submitAttempts = Math.max(1, tx.submitAttempts + 1))
            dao.insert(tx)
        }
    }

    override suspend fun getAll(): List<PendingTransaction> = withContext(IO) {
        dao.getAll()
    }

    private fun initTransaction(
        value: Long,
        toAddress: String,
        memo: String,
        currentHeight: Int = -1
    ): PendingTransaction {
        return PendingTransaction(
            value = value,
            toAddress = toAddress,
            memo = memo,
            expiryHeight = if (currentHeight == -1) -1 else currentHeight + EXPIRY_OFFSET
        )
    }

    suspend fun manageMined(pendingTx: PendingTransaction, matchingMinedTx: Transaction) = withContext(IO) {
        twig("a pending transaction has been mined!")
        val tx = pendingTx.copy(minedHeight = matchingMinedTx.minedHeight)
        dao.insert(tx)
    }

    /**
     * Remove a transaction and pretend it never existed.
     */
    suspend fun abortTransaction(existingTransaction: PendingTransaction) = withContext(IO) {
        dao.delete(existingTransaction)
    }

}