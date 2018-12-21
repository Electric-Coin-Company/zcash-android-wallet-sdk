package cash.z.wallet.sdk.dao

import androidx.room.*
import cash.z.wallet.sdk.vo.Block
import androidx.lifecycle.LiveData



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

}