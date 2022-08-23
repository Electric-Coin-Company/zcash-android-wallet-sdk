package cash.z.ecc.android.sdk.internal.db.pending

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import cash.z.ecc.android.sdk.internal.model.PendingTransaction
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork

@Entity(tableName = "pending_transactions")
data class PendingTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val toAddress: String,
    val value: Long,
    val memo: ByteArray?,
    val accountIndex: Int,
    val minedHeight: Long = NO_BLOCK_HEIGHT,
    val expiryHeight: Long = NO_BLOCK_HEIGHT,

    val cancelled: Int = 0,
    val encodeAttempts: Int = -1,
    val submitAttempts: Int = -1,
    val errorMessage: String? = null,
    val errorCode: Int? = null,
    val createTime: Long = System.currentTimeMillis(),
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val raw: ByteArray = byteArrayOf(),
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val rawTransactionId: ByteArray? = byteArrayOf()
) {
    fun toPendingTransaction(zcashNetwork: ZcashNetwork) = PendingTransaction(
        id = id,
        value = Zatoshi(value),
        memo = memo?.let { FirstClassByteArray(it) },
        raw = FirstClassByteArray(raw),
        toAddress = toAddress,
        accountIndex = accountIndex,
        minedHeight = if (minedHeight == NO_BLOCK_HEIGHT) {
            null
        } else {
            BlockHeight.new(zcashNetwork, minedHeight)
        },
        expiryHeight = if (expiryHeight == NO_BLOCK_HEIGHT) {
            null
        } else {
            BlockHeight.new(zcashNetwork, expiryHeight)
        },
        cancelled = cancelled,
        encodeAttempts = encodeAttempts,
        submitAttempts = submitAttempts,
        errorMessage = errorMessage,
        errorCode = errorCode,
        createTime = createTime,
        rawTransactionId = rawTransactionId?.let { FirstClassByteArray(it) }
    )

    @Suppress("ComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PendingTransactionEntity

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

    companion object {
        const val NO_BLOCK_HEIGHT = -1L

        fun from(pendingTransaction: PendingTransaction) = PendingTransactionEntity(
            id = pendingTransaction.id,
            value = pendingTransaction.value.value,
            memo = pendingTransaction.memo?.byteArray,
            raw = pendingTransaction.raw.byteArray,
            toAddress = pendingTransaction.toAddress,
            accountIndex = pendingTransaction.accountIndex,
            minedHeight = pendingTransaction.minedHeight?.value ?: NO_BLOCK_HEIGHT,
            expiryHeight = pendingTransaction.expiryHeight?.value ?: NO_BLOCK_HEIGHT,
            cancelled = pendingTransaction.cancelled,
            encodeAttempts = pendingTransaction.encodeAttempts,
            submitAttempts = pendingTransaction.submitAttempts,
            errorMessage = pendingTransaction.errorMessage,
            errorCode = pendingTransaction.errorCode,
            createTime = pendingTransaction.createTime,
            rawTransactionId = pendingTransaction.rawTransactionId?.byteArray
        )
    }
}

fun PendingTransactionEntity.isSubmitted(): Boolean {
    return submitAttempts > 0
}

fun PendingTransactionEntity.isFailedEncoding() = raw.isNotEmpty() && encodeAttempts > 0

fun PendingTransactionEntity.isCancelled(): Boolean {
    return cancelled > 0
}
