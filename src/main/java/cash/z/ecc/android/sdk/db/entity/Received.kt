package cash.z.ecc.android.sdk.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "received_notes",
    primaryKeys = ["id_note"],
    foreignKeys = [ForeignKey(
        entity = TransactionEntity::class,
        parentColumns = ["id_tx"],
        childColumns = ["tx"]
    ), ForeignKey(
        entity = Account::class,
        parentColumns = ["account"],
        childColumns = ["account"]
    ), ForeignKey(
        entity = TransactionEntity::class,
        parentColumns = ["id_tx"],
        childColumns = ["spent"]
    )]
)
data class Received(
    @ColumnInfo(name = "id_note")
    val id: Int? = 0,

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

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val diversifier: ByteArray = byteArrayOf(),

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val rcm: ByteArray = byteArrayOf(),

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val nf: ByteArray = byteArrayOf(),

    @ColumnInfo(name = "is_change")
    val isChange: Boolean = false,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val memo: ByteArray? = byteArrayOf()
) {

}
