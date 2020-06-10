package cash.z.ecc.android.sdk.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "sent_notes",
    primaryKeys = ["id_note"],
    foreignKeys = [ForeignKey(
        entity = TransactionEntity::class,
        parentColumns = ["id_tx"],
        childColumns = ["tx"]
    ), ForeignKey(
        entity = Account::class,
        parentColumns = ["account"],
        childColumns = ["from_account"]
    )]
)
data class Sent(
    @ColumnInfo(name = "id_note")
    val id: Int? = 0,

    @ColumnInfo(name = "tx")
    val transactionId: Int = 0,

    @ColumnInfo(name = "output_index")
    val outputIndex: Int = 0,

    @ColumnInfo(name = "from_account")
    val account: Int = 0,

    val address: String = "",

    val value: Long = 0,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val memo: ByteArray? = byteArrayOf()

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Sent) return false

        if (id != other.id) return false
        if (transactionId != other.transactionId) return false
        if (outputIndex != other.outputIndex) return false
        if (account != other.account) return false
        if (address != other.address) return false
        if (value != other.value) return false
        if (memo != null) {
            if (other.memo == null) return false
            if (!memo.contentEquals(other.memo)) return false
        } else if (other.memo != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + transactionId
        result = 31 * result + outputIndex
        result = 31 * result + account
        result = 31 * result + address.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + (memo?.contentHashCode() ?: 0)
        return result
    }

}
