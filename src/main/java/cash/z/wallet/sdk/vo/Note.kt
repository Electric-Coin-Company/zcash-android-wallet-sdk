package cash.z.wallet.sdk.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore

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
    val id: Int = 0,

    @ColumnInfo(name = "tx")
    val transaction: Int = 0,

    @ColumnInfo(name = "output_index")
    val outputIndex: Int = 0,

    val account: Int = 0,
    val value: Int = 0,
    val spent: Int? = 0,

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
        result = 31 * result + (spent ?: 0)
        result = 31 * result + diversifier.contentHashCode()
        result = 31 * result + rcm.contentHashCode()
        result = 31 * result + nf.contentHashCode()
        result = 31 * result + (memo?.contentHashCode() ?: 0)
        return result
    }

}

data class NoteQuery(val txId: Int, val value: Int, val height: Int, val sent: Boolean, val time: Long)