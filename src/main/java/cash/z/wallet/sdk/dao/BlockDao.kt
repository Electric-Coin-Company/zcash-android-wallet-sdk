package cash.z.wallet.sdk.dao

import androidx.room.*
import cash.z.wallet.sdk.entity.Block

@Dao
interface BlockDao {
    @Query("SELECT COUNT(height) FROM blocks")
    fun count(): Int

    @Query("SELECT MAX(height) FROM blocks")
    fun lastScannedHeight(): Int
}