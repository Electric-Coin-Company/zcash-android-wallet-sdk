package cash.z.ecc.android.sdk.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import org.jetbrains.annotations.NotNull

@Entity(primaryKeys = ["height"], tableName = "blocks")
data class Block(
    val height: Int?,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB, name = "hash")
    @NotNull
    val hash: ByteArray,
    @NotNull
    val time: Int,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB, name = "sapling_tree")
    @NotNull
    val saplingTree: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Block) return false

        if (height != other.height) return false
        if (!hash.contentEquals(other.hash)) return false
        if (time != other.time) return false
        if (!saplingTree.contentEquals(other.saplingTree)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = height ?: 0
        result = 31 * result + hash.contentHashCode()
        result = 31 * result + time
        result = 31 * result + saplingTree.contentHashCode()
        return result
    }
}
