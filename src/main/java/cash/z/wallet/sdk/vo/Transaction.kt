package cash.z.wallet.sdk.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import org.jetbrains.annotations.NotNull

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
    val id: Int,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB, name = "txid")
    @NotNull
    val transactionId: ByteArray,

    val block: Int,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val raw: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return (other is Transaction)
                && id == other.id
                && transactionId.contentEquals(other.transactionId)
                && block == other.block
                && ((raw == null && other.raw == null) || (raw != null && other.raw != null && raw.contentEquals(other.raw)))
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + transactionId.contentHashCode()
        result = 31 * result + block
        result = 31 * result + (raw?.contentHashCode() ?: 0)
        return result
    }

}