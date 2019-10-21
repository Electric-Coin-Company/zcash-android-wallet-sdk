package cash.z.wallet.sdk.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore

@Entity(
    tableName = "accounts",
    primaryKeys = ["account"]
)
data class Account(
    val account: Int? = 0,

    @ColumnInfo(name = "extfvk")
    val extendedFullViewingKey: String = "",

    val address: String = ""
)

