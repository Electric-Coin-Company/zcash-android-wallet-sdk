package cash.z.wallet.sdk.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["height"], tableName = "blocks")
data class Block(
    val height: Int,
    val time: Int?,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB, name = "sapling_tree")
    val saplingTree: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return (other is Block)
                && height == other.height
                && time == other.time
                && saplingTree.contentEquals(other.saplingTree)
    }

    override fun hashCode(): Int {
        var result = height
        result = 31 * result + (time ?: 0)
        result = 31 * result + saplingTree.contentHashCode()
        return result
    }


}