package cash.z.wallet.sdk.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import cash.z.wallet.sdk.data.SignedTransaction
import org.jetbrains.annotations.NotNull


//
// Entities
//

@Entity(
    primaryKeys = ["id_tx"], tableName = "transactions",
    foreignKeys = [ForeignKey(
        entity = Block::class,
        parentColumns = ["height"],
        childColumns = ["block"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Transaction(
    @ColumnInfo(name = "id_tx")
    val id: Long,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB, name = "txid")
    @NotNull
    val transactionId: ByteArray,

    @ColumnInfo(name = "tx_index")
    val transactionIndex: Int,

    @ColumnInfo(name = "expiry_height")
    val expiryHeight: Int,

    @ColumnInfo(name = "block")
    val minedHeight: Int,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val raw: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Transaction) return false

        if (id != other.id) return false
        if (!transactionId.contentEquals(other.transactionId)) return false
        if (transactionIndex != other.transactionIndex) return false
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
        result = 31 * result + transactionIndex
        result = 31 * result + expiryHeight
        result = 31 * result + minedHeight
        result = 31 * result + (raw?.contentHashCode() ?: 0)
        return result
    }
}

@Entity(tableName = "pending_transactions")
data class PendingTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val toAddress: String = "",
    val value: Long = -1,
    val memo: String? = null,
    val minedHeight: Int = -1,
    val expiryHeight: Int = -1,

    val encodeAttempts: Int = -1,
    val submitAttempts: Int = -1,
    val errorMessage: String? = null,
    val errorCode: Int? = null,
    val createTime: Long = System.currentTimeMillis(),

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    override val raw: ByteArray = ByteArray(0),
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val rawTransactionId: ByteArray? = null
) : SignedTransaction {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PendingTransaction) return false

        if (id != other.id) return false
        if (toAddress != other.toAddress) return false
        if (value != other.value) return false
        if (memo != other.memo) return false
        if (minedHeight != other.minedHeight) return false
        if (expiryHeight != other.expiryHeight) return false
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
        result = 31 * result + memo.hashCode()
        result = 31 * result + minedHeight
        result = 31 * result + expiryHeight
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
 * Parent type for transactions that have been mined. This is useful for putting all transactions in one list for things
 * like history. A mined tx should have all properties, except possibly a memo.
 */
interface ClearedTransaction {
    val id: Long
    val value: Long
//    val memo: String? --> we don't yet have a good way of privately retrieving incoming memos so let's make that clear
    val noteId: Long
    val minedHeight: Int
    val blockTimeInSeconds: Long
    val transactionIndex: Int
    val rawTransactionId: ByteArray
}

/**
 * A mined, inbound shielded transaction. Since this is a [ClearedTransaction], it represents data on the blockchain.
 */
data class ReceivedTransaction(
    override val id: Long = 0L,
    override val value: Long = 0L,
//    override val memo: String? = null, --> for now we don't have a good way of privately retrieving incoming memos so let's make that clear by omitting this property
    override val noteId: Long = 0L,
    override val blockTimeInSeconds: Long = 0L,
    override val minedHeight: Int = -1,
    override val transactionIndex: Int,
    override val rawTransactionId: ByteArray = ByteArray(0)
) : ClearedTransaction {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReceivedTransaction) return false

        if (id != other.id) return false
        if (value != other.value) return false
        if (noteId != other.noteId) return false
        if (blockTimeInSeconds != other.blockTimeInSeconds) return false
        if (minedHeight != other.minedHeight) return false
        if (transactionIndex != other.transactionIndex) return false
        if (!rawTransactionId.contentEquals(other.rawTransactionId)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + noteId.hashCode()
        result = 31 * result + blockTimeInSeconds.hashCode()
        result = 31 * result + minedHeight
        result = 31 * result + transactionIndex
        result = 31 * result + rawTransactionId.contentHashCode()
        return result
    }
}

/**
 * A mined, outbound shielded transaction. Since this is a [ClearedTransaction], it represents data on the blockchain.
 */
data class SentTransaction(
    override val id: Long = 0L,
    override val value: Long = 0L,
    override val noteId: Long = 0L,
    override val blockTimeInSeconds: Long = 0L,
    override val minedHeight: Int = -1,
    override val transactionIndex: Int,
    override val rawTransactionId: ByteArray = ByteArray(0),

    // sent transactions have memos because we create them and don't have to worry about P.I.R.
    val memo: String? = null,
    val toAddress: String = "",
    val expiryHeight: Int = -1,
    override val raw: ByteArray = ByteArray(0)
) : ClearedTransaction, SignedTransaction {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SentTransaction) return false

        if (id != other.id) return false
        if (value != other.value) return false
        if (noteId != other.noteId) return false
        if (blockTimeInSeconds != other.blockTimeInSeconds) return false
        if (minedHeight != other.minedHeight) return false
        if (transactionIndex != other.transactionIndex) return false
        if (!rawTransactionId.contentEquals(other.rawTransactionId)) return false
        if (memo != other.memo) return false
        if (toAddress != other.toAddress) return false
        if (expiryHeight != other.expiryHeight) return false
        if (!raw.contentEquals(other.raw)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + noteId.hashCode()
        result = 31 * result + blockTimeInSeconds.hashCode()
        result = 31 * result + minedHeight
        result = 31 * result + transactionIndex
        result = 31 * result + rawTransactionId.contentHashCode()
        result = 31 * result + (memo?.hashCode() ?: 0)
        result = 31 * result + toAddress.hashCode()
        result = 31 * result + expiryHeight
        result = 31 * result + raw.contentHashCode()
        return result
    }
}

data class EncodedTransaction(val txId: ByteArray, val raw: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncodedTransaction) return false

        if (!txId.contentEquals(other.txId)) return false
        if (!raw.contentEquals(other.raw)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = txId.contentHashCode()
        result = 31 * result + raw.contentHashCode()
        return result
    }
}

//
// Extension-oriented design
//

fun PendingTransaction.isSameTxId(other: ClearedTransaction): Boolean {
    return rawTransactionId != null && other.rawTransactionId != null && rawTransactionId.contentEquals(other.rawTransactionId)
}

fun PendingTransaction.isSameTxId(other: PendingTransaction): Boolean {
    return rawTransactionId != null && other.rawTransactionId != null && rawTransactionId.contentEquals(other.rawTransactionId)
}

fun PendingTransaction.isCreating(): Boolean {
    return raw.isEmpty() && submitAttempts <= 0 && !isFailedSubmit() && !isFailedEncoding()
}

fun PendingTransaction.isFailedEncoding(): Boolean {
    return raw.isEmpty() && encodeAttempts > 0
}

fun PendingTransaction.isFailedSubmit(): Boolean {
    return errorMessage != null || (errorCode != null && errorCode < 0)
}

fun PendingTransaction.isFailure(): Boolean {
    return isFailedEncoding() || isFailedSubmit()
}

fun PendingTransaction.isMined(): Boolean {
    return minedHeight > 0
}

fun PendingTransaction.isSubmitted(): Boolean {
    return submitAttempts > 0
}

fun PendingTransaction.isPending(currentHeight: Int = -1): Boolean {
    // not mined and not expired and successfully created
    return !isSubmitSuccess() && minedHeight == -1 && (expiryHeight == -1 || expiryHeight > currentHeight) && raw != null
}

fun PendingTransaction.isSubmitSuccess(): Boolean {
    return submitAttempts > 0 && (errorCode != null && errorCode >= 0) && errorMessage == null
}
