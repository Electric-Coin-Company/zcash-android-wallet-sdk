package cash.z.wallet.sdk.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "sent_notes",
    primaryKeys = ["id_note"],
    foreignKeys = [ForeignKey(
        entity = Transaction::class,
        parentColumns = ["id_tx"],
        childColumns = ["tx"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = Account::class,
        parentColumns = ["account"],
        childColumns = ["from_account"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.SET_NULL
    )]
)
data class Sent(
    @ColumnInfo(name = "id_note")
    val id: Int = 0,

    @ColumnInfo(name = "tx")
    val transactionId: Int = 0,

    @ColumnInfo(name = "output_index")
    val outputIndex: Int = 0,

    @ColumnInfo(name = "from_account")
    val account: Int = 0,

    val address: String,

    val value: Long = 0,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val memo: ByteArray? = byteArrayOf()

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return (other is Sent)
                && id == other.id
                && transactionId == other.transactionId
                && outputIndex == other.outputIndex
                && account == other.account
                && value == other.value
                && ((memo == null && other.memo == null) || (memo != null && other.memo != null && memo.contentEquals(other.memo)))
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + transactionId
        result = 31 * result + outputIndex
        result = 31 * result + account
        result = 31 * result + value.toInt()
        result = 31 * result + (memo?.contentHashCode() ?: 0)
        return result
    }

}
