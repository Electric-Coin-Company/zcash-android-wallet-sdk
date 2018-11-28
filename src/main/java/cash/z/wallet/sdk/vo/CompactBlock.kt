package cash.z.wallet.sdk.vo

import androidx.room.Entity

@Entity(primaryKeys = ["height"])
data class CompactBlock(
    val height: Int/*,
    val data: Any*/
)