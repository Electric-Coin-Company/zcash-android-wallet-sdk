package cash.z.ecc.android.sdk.internal.db.block

import androidx.room.ColumnInfo
import androidx.room.Entity
import cash.z.ecc.android.sdk.internal.model.CompactBlock
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.ZcashNetwork

@Entity(primaryKeys = ["height"], tableName = "compactblocks")
data class CompactBlockEntity(
    val height: Long,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompactBlockEntity) return false

        if (height != other.height) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = height.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }

    fun toCompactBlock(zcashNetwork: ZcashNetwork) = CompactBlock(
        BlockHeight.new(zcashNetwork, height),
        FirstClassByteArray(data)
    )

    companion object {
        fun fromCompactBlock(compactBlock: CompactBlock) =
            CompactBlockEntity(compactBlock.height.value, compactBlock.data.byteArray)
    }
}
