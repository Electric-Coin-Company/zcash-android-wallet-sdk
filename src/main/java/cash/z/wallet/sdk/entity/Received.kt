package cash.z.wallet.sdk.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "received_notes",
    primaryKeys = ["id_note"],
    foreignKeys = [ForeignKey(
        entity = Transaction::class,
        parentColumns = ["id_tx"],
        childColumns = ["tx"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = Transaction::class,
        parentColumns = ["id_tx"],
        childColumns = ["spent"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.SET_NULL
    ), ForeignKey(
        entity = Account::class,
        parentColumns = ["account"],
        childColumns = ["account"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE
    )]
)
data class Received(
    @ColumnInfo(name = "id_note")
    val id: Int = 0,

    /**
     * A reference to the transaction this note was received in
     */
    @ColumnInfo(name = "tx")
    val transactionId: Int = 0,

    @ColumnInfo(name = "output_index")
    val outputIndex: Int = 0,

    val account: Int = 0,
    val value: Long = 0,

    /**
     * A reference to the transaction this note was later spent in
     */
    val spent: Int? = 0,

    @ColumnInfo(name = "is_change")
    val isChange: Boolean = false,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val diversifier: ByteArray = byteArrayOf(),

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val rcm: ByteArray = byteArrayOf(),

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val nf: ByteArray = byteArrayOf(),

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val memo: ByteArray? = byteArrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return (other is Received)
                && id == other.id
                && transactionId == other.transactionId
                && outputIndex == other.outputIndex
                && account == other.account
                && value == other.value
                && spent == other.spent
                && diversifier.contentEquals(other.diversifier)
                && rcm.contentEquals(other.rcm)
                && nf.contentEquals(other.nf)
                && isChange == other.isChange
                && ((memo == null && other.memo == null)
                || (memo != null && other.memo != null && memo.contentEquals(other.memo)))
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + transactionId
        result = 31 * result + outputIndex
        result = 31 * result + account
        result = 31 * result + value.toInt()
        result = 31 * result + (if (isChange) 1 else 0)
        result = 31 * result + (spent ?: 0)
        result = 31 * result + diversifier.contentHashCode()
        result = 31 * result + rcm.contentHashCode()
        result = 31 * result + nf.contentHashCode()
        result = 31 * result + (memo?.contentHashCode() ?: 0)
        return result
    }

}
