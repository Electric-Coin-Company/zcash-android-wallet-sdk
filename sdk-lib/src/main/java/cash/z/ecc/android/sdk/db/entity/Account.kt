package cash.z.ecc.android.sdk.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "accounts",
    primaryKeys = ["account"]
)
data class Account(

    val account: Int? = 0,

    @ColumnInfo(name = "ufvk")
    val unifiedFullViewingKey: String? = "",

    val address: String? = "",

    @ColumnInfo(name = "transparent_address")
    val transparentAddress: String? = ""
)
