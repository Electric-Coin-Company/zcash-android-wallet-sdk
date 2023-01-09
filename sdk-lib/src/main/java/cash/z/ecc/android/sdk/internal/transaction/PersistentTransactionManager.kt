package cash.z.ecc.android.sdk.internal.transaction

import android.content.Context
import androidx.room.RoomDatabase
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.db.commonDatabaseBuilder
import cash.z.ecc.android.sdk.internal.db.pending.PendingTransactionDao
import cash.z.ecc.android.sdk.internal.db.pending.PendingTransactionDb
import cash.z.ecc.android.sdk.internal.db.pending.PendingTransactionEntity
import cash.z.ecc.android.sdk.internal.db.pending.isCancelled
import cash.z.ecc.android.sdk.internal.db.pending.isFailedEncoding
import cash.z.ecc.android.sdk.internal.db.pending.isSubmitted
import cash.z.ecc.android.sdk.internal.db.pending.recipient
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.PendingTransaction
import cash.z.ecc.android.sdk.model.TransactionRecipient
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.BlockingLightWalletClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Facilitates persistent attempts to ensure that an outbound transaction is completed.
 *
 * @param db the database where the wallet can freely write information related to pending
 * transactions. This database effectively serves as the mempool for transactions created by this
 * wallet.
 * @property encoder responsible for encoding a transaction by taking all the inputs and returning
 * an [cash.z.ecc.android.sdk.entity.EncodedTransaction] object containing the raw bytes and transaction
 * id.
 * @property service the lightwallet service used to submit transactions.
 */
@Suppress("TooManyFunctions")
internal class PersistentTransactionManager(
    db: PendingTransactionDb,
    private val zcashNetwork: ZcashNetwork,
    internal val encoder: TransactionEncoder,
    private val service: BlockingLightWalletClient
) : OutboundTransactionManager {

    private val daoMutex = Mutex()

    /**
     * Internal reference to the dao that is only accessed after locking the [daoMutex] in order
     * to enforce DB access in both a threadsafe and coroutinesafe way.
     */
    private val _dao: PendingTransactionDao = db.pendingTransactionDao()

    //
    // OutboundTransactionManager implementation
    //

    override suspend fun initSpend(
        zatoshi: Zatoshi,
        recipient: TransactionRecipient,
        memo: String,
        account: Account
    ): PendingTransaction = withContext(Dispatchers.IO) {
        twig("constructing a placeholder transaction")

        val toAddress = if (recipient is TransactionRecipient.Address) {
            recipient.addressValue
        } else {
            null
        }
        val toInternal = if (recipient is TransactionRecipient.Account) {
            recipient.accountValue
        } else {
            null
        }

        var tx = PendingTransactionEntity(
            toAddress = toAddress,
            toInternalAccountIndex = toInternal?.value,
            value = zatoshi.value,
            fee = ZcashSdk.MINERS_FEE.value,
            memo = memo.toByteArray(),
            sentFromAccountIndex = account.value
        )
        @Suppress("TooGenericExceptionCaught")
        try {
            safeUpdate("creating tx in DB") {
                tx = findById(create(tx))!!
                twig("successfully created TX in DB with id: ${tx.id}")
            }
        } catch (t: Throwable) {
            twig(
                "Unknown error while attempting to create and fetch pending transaction:" +
                    " ${t.message} caused by: ${t.cause}"
            )
        }

        tx.toPendingTransaction(zcashNetwork)
    }

    override suspend fun applyMinedHeight(pendingTx: PendingTransaction, minedHeight: BlockHeight) {
        twig("a pending transaction has been mined!")
        safeUpdate("updating mined height for pending tx id: ${pendingTx.id} to $minedHeight") {
            updateMinedHeight(pendingTx.id, minedHeight.value)
        }
    }

    override suspend fun encode(
        usk: UnifiedSpendingKey,
        pendingTx: PendingTransaction
    ): PendingTransaction = withContext(Dispatchers.IO) {
        twig("managing the creation of a transaction")

        var tx = PendingTransactionEntity.from(pendingTx)

        @Suppress("TooGenericExceptionCaught")
        try {
            twig("beginning to encode transaction with : $encoder")
            val encodedTx = encoder.createTransaction(
                usk,
                pendingTx.value,
                pendingTx.recipient,
                pendingTx.memo?.byteArray
            )
            twig("successfully encoded transaction!")
            safeUpdate("updating transaction encoding", -1) {
                updateEncoding(
                    pendingTx.id,
                    encodedTx.raw.byteArray,
                    encodedTx.txId.byteArray,
                    encodedTx
                        .expiryHeight?.value
                )
            }
        } catch (t: Throwable) {
            var message = "failed to encode transaction due to : ${t.message}"
            t.cause?.let { message += " caused by: $it" }
            twig(message)
            safeUpdate("updating transaction error info") {
                updateError(pendingTx.id, message, ERROR_ENCODING)
            }
        } finally {
            safeUpdate("incrementing transaction encodeAttempts (from: ${tx.encodeAttempts})", -1) {
                updateEncodeAttempts(tx.id, max(1, tx.encodeAttempts + 1))
                tx = findById(tx.id)!!
            }
        }

        tx.toPendingTransaction(zcashNetwork)
    }

    // TODO(str4d): This uses operator overloading to distinguish between it and createToAddress, which breaks when
    //  spendingKey is removed. Figure out where these methods need to be renamed, and do so.
    override suspend fun encode(
        spendingKey: String, // TODO(str4d): Remove this argument.
        usk: UnifiedSpendingKey,
        pendingTx: PendingTransaction
    ): PendingTransaction {
        twig("managing the creation of a shielding transaction")
        var tx = PendingTransactionEntity.from(pendingTx)
        @Suppress("TooGenericExceptionCaught")
        try {
            twig("beginning to encode shielding transaction with : $encoder")
            val encodedTx = encoder.createShieldingTransaction(
                usk,
                tx.recipient,
                tx.memo
            )
            twig("successfully encoded shielding transaction!")
            safeUpdate("updating shielding transaction encoding") {
                updateEncoding(tx.id, encodedTx.raw.byteArray, encodedTx.txId.byteArray, encodedTx.expiryHeight?.value)
            }
        } catch (t: Throwable) {
            var message = "failed to encode auto-shielding transaction due to : ${t.message}"
            t.cause?.let { message += " caused by: $it" }
            twig(message)
            safeUpdate("updating shielding transaction error info") {
                updateError(tx.id, message, ERROR_ENCODING)
            }
        } finally {
            safeUpdate("incrementing shielding transaction encodeAttempts (from: ${tx.encodeAttempts})") {
                updateEncodeAttempts(tx.id, max(1, tx.encodeAttempts + 1))
                tx = findById(tx.id)!!
            }
        }

        return tx.toPendingTransaction(zcashNetwork)
    }

    override suspend fun submit(pendingTx: PendingTransaction): PendingTransaction = withContext(Dispatchers.IO) {
        // reload the tx to check for cancellation
        var tx = pendingTransactionDao { findById(pendingTx.id) }
            ?: throw IllegalStateException(
                "Error while submitting transaction. No pending" +
                    " transaction found that matches the one being submitted. Verify that the" +
                    " transaction still exists among the set of pending transactions."
            )
        @Suppress("TooGenericExceptionCaught")
        try {
            // do nothing if failed or cancelled
            when {
                tx.isFailedEncoding() ->
                    twig("Warning: this transaction will not be submitted because it failed to be encoded.")
                tx.isCancelled() ->
                    twig(
                        "Warning: ignoring cancelled transaction with id ${tx.id}. We will not submit it to" +
                            " the network because it has been cancelled."
                    )
                else -> {
                    twig("submitting transaction with memo: ${tx.memo} amount: ${tx.value}", -1)
                    val response = service.submitTransaction(tx.raw)
                    val error = response.errorCode < 0
                    twig(
                        "${if (error) "FAILURE! " else "SUCCESS!"} submit transaction completed with" +
                            " response: ${response.errorCode}: ${response.errorMessage}"
                    )

                    safeUpdate("updating submitted transaction (hadError: $error)", -1) {
                        updateError(tx.id, if (error) response.errorMessage else null, response.errorCode)
                        updateSubmitAttempts(tx.id, max(1, tx.submitAttempts + 1))
                    }
                }
            }
        } catch (t: Throwable) {
            // a non-server error has occurred
            var message =
                "Unknown error while submitting transaction: ${t.message}"
            t.cause?.let { message += " caused by: $it" }
            twig(message)
            safeUpdate("updating submission failure") {
                updateError(tx.id, t.message, ERROR_SUBMITTING)
                updateSubmitAttempts(tx.id, max(1, tx.submitAttempts + 1))
            }
        } finally {
            safeUpdate("fetching latest tx info", -1) {
                tx = findById(tx.id)!!
            }
        }

        tx.toPendingTransaction(zcashNetwork)
    }

    override suspend fun monitorById(id: Long): Flow<PendingTransaction> {
        return pendingTransactionDao { monitorById(id) }.map { it.toPendingTransaction(zcashNetwork) }
    }

    override suspend fun isValidShieldedAddress(address: String) =
        encoder.isValidShieldedAddress(address)

    override suspend fun isValidTransparentAddress(address: String) =
        encoder.isValidTransparentAddress(address)

    override suspend fun isValidUnifiedAddress(address: String) =
        encoder.isValidUnifiedAddress(address)

    override suspend fun cancel(pendingId: Long): Boolean {
        return pendingTransactionDao {
            val tx = findById(pendingId)
            if (tx?.isSubmitted() == true) {
                twig("Attempt to cancel transaction failed because it has already been submitted!")
                false
            } else {
                twig("Cancelling unsubmitted transaction id: $pendingId")
                cancel(pendingId)
                true
            }
        }
    }

    override suspend fun findById(id: Long) = pendingTransactionDao {
        findById(id)
    }?.toPendingTransaction(zcashNetwork)

    override suspend fun markForDeletion(id: Long) = pendingTransactionDao {
        withContext(IO) {
            twig("[cleanup] marking pendingTx $id for deletion")
            removeRawTransactionId(id)
            updateError(
                id,
                SAFE_TO_DELETE_ERROR_MESSAGE,
                SAFE_TO_DELETE_ERROR_CODE
            )
        }
    }

    /**
     * Remove a transaction and pretend it never existed.
     *
     * @param transaction the transaction to be processed.
     *
     * @return the final number of transactions that were removed from the database.
     */
    override suspend fun abort(transaction: PendingTransaction): Int {
        return pendingTransactionDao {
            twig("[cleanup] Deleting pendingTxId: ${transaction.id}")
            delete(PendingTransactionEntity.from(transaction))
        }
    }

    override fun getAll() = _dao.getAll().map { list -> list.map { it.toPendingTransaction(zcashNetwork) } }

    //
    // Helper functions
    //

    /**
     * Updating the pending transaction is often done at the end of a function but still should happen within a
     * try/catch block, surrounded by logging. So this helps with that while also ensuring that no other coroutines are
     * concurrently interacting with the DAO.
     */
    private suspend fun <R> safeUpdate(
        logMessage: String = "",
        priority: Int = 0,
        block: suspend PendingTransactionDao.() -> R
    ): R? {
        @Suppress("TooGenericExceptionCaught")
        return try {
            twig(logMessage, priority)
            pendingTransactionDao { block() }
        } catch (t: Throwable) {
            twig(
                "Unknown error while attempting to '$logMessage':" +
                    " ${t.message} caused by: ${t.cause} stacktrace: ${t.stackTrace}",
                priority
            )
            null
        }
    }

    private suspend fun <T> pendingTransactionDao(block: suspend PendingTransactionDao.() -> T): T {
        return daoMutex.withLock {
            withContext(IO) {
                _dao.block()
            }
        }
    }

    companion object {
        /** Error code for an error while encoding a transaction */
        const val ERROR_ENCODING = 2000

        /** Error code for an error while submitting a transaction */
        const val ERROR_SUBMITTING = 3000

        private const val SAFE_TO_DELETE_ERROR_MESSAGE = "safe to delete"
        const val SAFE_TO_DELETE_ERROR_CODE = -9090

        fun new(
            appContext: Context,
            zcashNetwork: ZcashNetwork,
            encoder: TransactionEncoder,
            service: BlockingLightWalletClient,
            databaseFile: File
        ) = PersistentTransactionManager(
            commonDatabaseBuilder(
                appContext,
                PendingTransactionDb::class.java,
                databaseFile
            ).setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .addMigrations(PendingTransactionDb.MIGRATION_1_2).build(),
            zcashNetwork,
            encoder,
            service
        )
    }
}
