@file:Suppress("TooManyFunctions")

package cash.z.ecc.android.sdk.db.entity

import android.text.format.DateUtils
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.RoomWarnings
import cash.z.ecc.android.sdk.internal.transaction.PersistentTransactionManager
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.Zatoshi

//
// Entities
//

@Entity(
    primaryKeys = ["id_tx"],
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Block::class,
            parentColumns = ["height"],
            childColumns = ["block"]
        )
    ]
)
@SuppressWarnings(RoomWarnings.MISSING_INDEX_ON_FOREIGN_KEY_CHILD)
data class TransactionEntity(
    @ColumnInfo(name = "id_tx")
    val id: Long?,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB, name = "txid")
    val transactionId: ByteArray,

    @ColumnInfo(name = "tx_index")
    val transactionIndex: Int?,

    val created: String?,

    @ColumnInfo(name = "expiry_height")
    val expiryHeight: Int?,

    @ColumnInfo(name = "block")
    val minedHeight: Int?,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val raw: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransactionEntity) return false

        if (id != other.id) return false
        if (!transactionId.contentEquals(other.transactionId)) return false
        if (transactionIndex != other.transactionIndex) return false
        if (created != other.created) return false
        if (expiryHeight != other.expiryHeight) return false
        if (minedHeight != other.minedHeight) return false
        if (raw != null) {
            if (other.raw == null) return false
            if (!raw.contentEquals(other.raw)) return false
        } else if (other.raw != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + transactionId.contentHashCode()
        result = 31 * result + (transactionIndex ?: 0)
        result = 31 * result + (created?.hashCode() ?: 0)
        result = 31 * result + (expiryHeight ?: 0)
        result = 31 * result + (minedHeight ?: 0)
        result = 31 * result + (raw?.contentHashCode() ?: 0)
        return result
    }
}

@Entity(tableName = "pending_transactions")
data class PendingTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    override val toAddress: String = "",
    override val value: Long = -1,
    override val memo: ByteArray? = byteArrayOf(),
    override val accountIndex: Int,
    override val minedHeight: Long = -1,
    override val expiryHeight: Long = -1,

    override val cancelled: Int = 0,
    override val encodeAttempts: Int = -1,
    override val submitAttempts: Int = -1,
    override val errorMessage: String? = null,
    override val errorCode: Int? = null,
    override val createTime: Long = System.currentTimeMillis(),

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    override val raw: ByteArray = byteArrayOf(),
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    override val rawTransactionId: ByteArray? = byteArrayOf()
) : PendingTransaction {

    val valueZatoshi: Zatoshi
        get() = Zatoshi(value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PendingTransactionEntity) return false

        if (id != other.id) return false
        if (toAddress != other.toAddress) return false
        if (value != other.value) return false
        if (memo != null) {
            if (other.memo == null) return false
            if (!memo.contentEquals(other.memo)) return false
        } else if (other.memo != null) return false
        if (accountIndex != other.accountIndex) return false
        if (minedHeight != other.minedHeight) return false
        if (expiryHeight != other.expiryHeight) return false
        if (cancelled != other.cancelled) return false
        if (encodeAttempts != other.encodeAttempts) return false
        if (submitAttempts != other.submitAttempts) return false
        if (errorMessage != other.errorMessage) return false
        if (errorCode != other.errorCode) return false
        if (createTime != other.createTime) return false
        if (!raw.contentEquals(other.raw)) return false
        if (rawTransactionId != null) {
            if (other.rawTransactionId == null) return false
            if (!rawTransactionId.contentEquals(other.rawTransactionId)) return false
        } else if (other.rawTransactionId != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + toAddress.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + (memo?.contentHashCode() ?: 0)
        result = 31 * result + accountIndex
        result = 31 * result + minedHeight.hashCode()
        result = 31 * result + expiryHeight.hashCode()
        result = 31 * result + cancelled
        result = 31 * result + encodeAttempts
        result = 31 * result + submitAttempts
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + (errorCode ?: 0)
        result = 31 * result + createTime.hashCode()
        result = 31 * result + raw.contentHashCode()
        result = 31 * result + (rawTransactionId?.contentHashCode() ?: 0)
        return result
    }
}

//
// Query Objects
//

/**
 * A mined, shielded transaction. Since this is a [MinedTransaction], it represents data
 * on the blockchain.
 */
data class ConfirmedTransaction(
    override val id: Long = 0L,
    override val value: Long = 0L,
    override val memo: ByteArray? = ByteArray(0),
    override val noteId: Long = 0L,
    override val blockTimeInSeconds: Long = 0L,
    override val minedHeight: Long = -1,
    override val transactionIndex: Int,
    override val rawTransactionId: ByteArray = ByteArray(0),

    // properties that differ from received transactions
    val toAddress: String? = null,
    val expiryHeight: Int? = null,
    override val raw: ByteArray? = byteArrayOf()
) : MinedTransaction, SignedTransaction {

    val minedBlockHeight
        get() = if (minedHeight == -1L) {
            null
        } else {
            BlockHeight(minedHeight)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConfirmedTransaction) return false

        if (id != other.id) return false
        if (value != other.value) return false
        if (memo != null) {
            if (other.memo == null) return false
            if (!memo.contentEquals(other.memo)) return false
        } else if (other.memo != null) return false
        if (noteId != other.noteId) return false
        if (blockTimeInSeconds != other.blockTimeInSeconds) return false
        if (minedHeight != other.minedHeight) return false
        if (transactionIndex != other.transactionIndex) return false
        if (!rawTransactionId.contentEquals(other.rawTransactionId)) return false
        if (toAddress != other.toAddress) return false
        if (expiryHeight != other.expiryHeight) return false
        if (raw != null) {
            if (other.raw == null) return false
            if (!raw.contentEquals(other.raw)) return false
        } else if (other.raw != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + (memo?.contentHashCode() ?: 0)
        result = 31 * result + noteId.hashCode()
        result = 31 * result + blockTimeInSeconds.hashCode()
        result = 31 * result + minedHeight.hashCode()
        result = 31 * result + transactionIndex
        result = 31 * result + rawTransactionId.contentHashCode()
        result = 31 * result + (toAddress?.hashCode() ?: 0)
        result = 31 * result + (expiryHeight ?: 0)
        result = 31 * result + (raw?.contentHashCode() ?: 0)
        return result
    }
}

val ConfirmedTransaction.valueInZatoshi
    get() = Zatoshi(value)

data class EncodedTransaction(val txId: ByteArray, override val raw: ByteArray, val expiryHeight: Long?) :
    SignedTransaction {

    val expiryBlockHeight
        get() = expiryHeight?.let { BlockHeight(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncodedTransaction) return false

        if (!txId.contentEquals(other.txId)) return false
        if (!raw.contentEquals(other.raw)) return false
        if (expiryHeight != other.expiryHeight) return false

        return true
    }

    override fun hashCode(): Int {
        var result = txId.contentHashCode()
        result = 31 * result + raw.contentHashCode()
        result = 31 * result + (expiryHeight?.hashCode() ?: 0)
        return result
    }
}

//
// Transaction Interfaces
//

/**
 * Common interface between confirmed transactions on the blockchain and pending transactions being
 * constructed.
 */
interface Transaction {
    val id: Long
    val value: Long
    val memo: ByteArray?
}

/**
 * Interface for anything that's able to provide signed transaction bytes.
 */
interface SignedTransaction {
    val raw: ByteArray?
}

/**
 * Parent type for transactions that have been mined. This is useful for putting all transactions in
 * one list for things like history. A mined tx should have all properties, except possibly a memo.
 */
interface MinedTransaction : Transaction {
    val minedHeight: Long
    val noteId: Long
    val blockTimeInSeconds: Long
    val transactionIndex: Int
    val rawTransactionId: ByteArray
}

interface PendingTransaction : SignedTransaction, Transaction {
    override val id: Long
    override val value: Long
    override val memo: ByteArray?
    val toAddress: String
    val accountIndex: Int
    val minedHeight: Long // apparently this can be -1 as an uninitialized value
    val expiryHeight: Long // apparently this can be -1 as an uninitialized value
    val cancelled: Int
    val encodeAttempts: Int
    val submitAttempts: Int
    val errorMessage: String?
    val errorCode: Int?
    val createTime: Long
    val rawTransactionId: ByteArray?
}

//
// Extension-oriented design
//

fun PendingTransaction.isSameTxId(other: MinedTransaction): Boolean {
    return rawTransactionId != null && rawTransactionId!!.contentEquals(other.rawTransactionId)
}

fun PendingTransaction.isSameTxId(other: PendingTransaction): Boolean {
    return rawTransactionId != null && other.rawTransactionId != null &&
        rawTransactionId!!.contentEquals(other.rawTransactionId!!)
}

fun PendingTransaction.hasRawTransactionId(): Boolean {
    return rawTransactionId != null && (rawTransactionId?.isNotEmpty() == true)
}

fun PendingTransaction.isCreating(): Boolean {
    return (raw?.isEmpty() != false) && submitAttempts <= 0 && !isFailedSubmit() && !isFailedEncoding()
}

fun PendingTransaction.isCreated(): Boolean {
    return (raw?.isEmpty() == false) && submitAttempts <= 0 && !isFailedSubmit() && !isFailedEncoding()
}

fun PendingTransaction.isFailedEncoding(): Boolean {
    return (raw?.isEmpty() != false) && encodeAttempts > 0
}

fun PendingTransaction.isFailedSubmit(): Boolean {
    return errorMessage != null || (errorCode != null && errorCode!! < 0)
}

fun PendingTransaction.isFailure(): Boolean {
    return isFailedEncoding() || isFailedSubmit()
}

fun PendingTransaction.isCancelled(): Boolean {
    return cancelled > 0
}

fun PendingTransaction.isMined(): Boolean {
    return minedHeight > 0
}

fun PendingTransaction.isSubmitted(): Boolean {
    return submitAttempts > 0
}

fun PendingTransaction.isExpired(latestHeight: BlockHeight?, saplingActivationHeight: BlockHeight): Boolean {
    // TODO: test for off-by-one error here. Should we use <= or <
    if (latestHeight == null ||
        latestHeight.value < saplingActivationHeight.value ||
        expiryHeight < saplingActivationHeight.value
    ) {
        return false
    }
    return expiryHeight < latestHeight.value
}

// if we don't have info on a pendingtx after 100 blocks then it's probably safe to stop polling!
@Suppress("MagicNumber")
fun PendingTransaction.isLongExpired(latestHeight: BlockHeight?, saplingActivationHeight: BlockHeight): Boolean {
    if (latestHeight == null ||
        latestHeight.value < saplingActivationHeight.value ||
        expiryHeight < saplingActivationHeight.value
    ) {
        return false
    }
    return (latestHeight.value - expiryHeight) > 100
}

fun PendingTransaction.isMarkedForDeletion(): Boolean {
    return rawTransactionId == null &&
        (errorCode ?: 0) == PersistentTransactionManager.SAFE_TO_DELETE_ERROR_CODE
}

@Suppress("MagicNumber")
fun PendingTransaction.isSafeToDiscard(): Boolean {
    // invalid dates shouldn't happen or should be temporary
    if (createTime < 0) return false

    val age = System.currentTimeMillis() - createTime
    val smallThreshold = 30 * DateUtils.MINUTE_IN_MILLIS
    val hugeThreshold = 30 * DateUtils.DAY_IN_MILLIS
    return when {
        // if it is mined, then it is not pending so it can be deleted fairly quickly from this db
        isMined() && age > smallThreshold -> true
        // if a tx fails to encode, then there's not much we can do with it
        isFailedEncoding() && age > smallThreshold -> true
        // don't delete failed submissions until they've been cleaned up, properly, or else we lose
        // the ability to remove them in librustzcash prior to expiration
        isFailedSubmit() && isMarkedForDeletion() -> true
        !isMined() && age > hugeThreshold -> true
        else -> false
    }
}

fun PendingTransaction.isPending(currentHeight: BlockHeight?): Boolean {
    // not mined and not expired and successfully created
    return !isSubmitSuccess() && minedHeight == -1L &&
        (expiryHeight == -1L || expiryHeight > (currentHeight?.value ?: 0L)) && raw != null
}

fun PendingTransaction.isSubmitSuccess(): Boolean {
    return submitAttempts > 0 && (errorCode != null && errorCode!! >= 0) && errorMessage == null
}
