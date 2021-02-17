package cash.z.ecc.android.sdk.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "utxos")
data class UtxoEntity(
    val address: String ="",

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val txid: ByteArray? = byteArrayOf(),

    @ColumnInfo(name = "tx_index")
    val transactionIndex: Int? = -1,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val script: ByteArray? = byteArrayOf(),

    val value: Long = 0L,

    val height: Int? = -1,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UtxoEntity) return false

        if (id != other.id) return false
        if (address != other.address) return false
        if (txid != null) {
            if (other.txid == null) return false
            if (!txid.contentEquals(other.txid)) return false
        } else if (other.txid != null) return false
        if (transactionIndex != other.transactionIndex) return false
        if (script != null) {
            if (other.script == null) return false
            if (!script.contentEquals(other.script)) return false
        } else if (other.script != null) return false
        if (value != other.value) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + (txid?.contentHashCode() ?: 0)
        result = 31 * result + (transactionIndex ?: 0)
        result = 31 * result + (script?.contentHashCode() ?: 0)
        result = 31 * result + value.hashCode()
        result = 31 * result + (height ?: 0)
        return result
    }

}
