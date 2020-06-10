package cash.z.ecc.android.sdk.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

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

