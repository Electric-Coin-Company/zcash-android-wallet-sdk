package cash.z.wallet.sdk.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["height"], tableName = "compactblocks")
data class CompactBlock(
    val height: Int,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompactBlock) return false

        if (height != other.height) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = height
        result = 31 * result + data.contentHashCode()
        return result
    }
}