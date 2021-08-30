package cash.z.ecc.android.sdk.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "utxos",
    primaryKeys = ["id_utxo"],
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id_tx"],
            childColumns = ["spent_in_tx"]
        )
    ]
)
data class Utxo(
    @ColumnInfo(name = "id_utxo")
    val id: Long? = 0L,

    val address: String = "",

    @ColumnInfo(name = "prevout_txid", typeAffinity = ColumnInfo.BLOB)
    val txid: ByteArray = byteArrayOf(),

    @ColumnInfo(name = "prevout_idx")
    val transactionIndex: Int = -1,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val script: ByteArray = byteArrayOf(),

    @ColumnInfo(name = "value_zat")
    val value: Long = 0L,

    val height: Int = -1,

    /**
     * A reference to the transaction this note was later spent in
     */
    @ColumnInfo(name = "spent_in_tx")
    val spent: Int? = 0,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Utxo) return false

        if (id != other.id) return false
        if (address != other.address) return false
        if (!txid.contentEquals(other.txid)) return false
        if (transactionIndex != other.transactionIndex) return false
        if (!script.contentEquals(other.script)) return false
        if (value != other.value) return false
        if (height != other.height) return false
        if (spent != other.spent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + txid.contentHashCode()
        result = 31 * result + transactionIndex
        result = 31 * result + script.contentHashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + height
        result = 31 * result + (spent ?: 0)
        return result
    }
}
