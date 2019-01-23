package cash.z.wallet.sdk.dao

import androidx.room.*
import cash.z.wallet.sdk.vo.Block

@Dao
interface BlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(block: Block)

    @Query("SELECT * FROM blocks WHERE height = :height")
    fun findById(height: Int): Block?

    @Query("DELETE FROM blocks WHERE height = :height")
    fun deleteById(height: Int)

    @Delete
    fun delete(block: Block)

    @Query("SELECT COUNT(height) FROM blocks")
    fun count(): Int

    @Query("DELETE FROM blocks")
    fun deleteAll()

    @Query("SELECT MAX(height) FROM blocks")
    fun lastScannedHeight(): Long

    @Query("UPDATE blocks SET time=:time WHERE height = :height")
    fun updateTime(height: Int, time: Int)
}