package cash.z.wallet.sdk.vo

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
    )]
)
data class Note(
    @ColumnInfo(name = "id_note")
    val id: Int,

    @ColumnInfo(name = "tx")
    val transaction: Int,

    @ColumnInfo(name = "output_index")
    val outputIndex: Int,

    val account: Int,
    val value: Int,
    val spent: Int,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val diversifier: ByteArray,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val rcm: ByteArray,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val nf: ByteArray,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val memo: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return (other is Note)
                && id == other.id
                && transaction == other.transaction
                && outputIndex == other.outputIndex
                && account == other.account
                && value == other.value
                && spent == other.spent
                && diversifier.contentEquals(other.diversifier)
                && rcm.contentEquals(other.rcm)
                && nf.contentEquals(other.nf)
                && ((memo == null && other.memo == null) || (memo != null && other.memo != null && memo.contentEquals(other.memo)))
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + transaction
        result = 31 * result + outputIndex
        result = 31 * result + account
        result = 31 * result + value
        result = 31 * result + spent
        result = 31 * result + diversifier.contentHashCode()
        result = 31 * result + rcm.contentHashCode()
        result = 31 * result + nf.contentHashCode()
        result = 31 * result + (memo?.contentHashCode() ?: 0)
        return result
    }

}