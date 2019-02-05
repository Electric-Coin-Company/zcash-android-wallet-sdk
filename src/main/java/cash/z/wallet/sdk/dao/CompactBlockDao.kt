package cash.z.wallet.sdk.dao

import androidx.room.*
import cash.z.wallet.sdk.entity.CompactBlock

@Dao
interface CompactBlockDao {

    @Query("SELECT COUNT(height) FROM compactblocks")
    fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(block: CompactBlock)

    @Query("SELECT * FROM compactblocks WHERE height = :height")
    fun findById(height: Int): CompactBlock?

    @Query("DELETE FROM compactblocks WHERE height = :height")
    fun deleteById(height: Int)

    @Delete
    fun delete(block: CompactBlock)

    @Query("SELECT MAX(height) FROM compactblocks")
    fun latestBlockHeight(): Int
}