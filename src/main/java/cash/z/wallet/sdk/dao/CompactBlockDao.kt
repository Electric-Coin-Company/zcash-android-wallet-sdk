package cash.z.wallet.sdk.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.z.wallet.sdk.vo.CompactBlock

@Dao
interface CompactBlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(block: CompactBlock)

    @Query("SELECT * FROM CompactBlock WHERE height = :height")
    fun findById(height: Int): CompactBlock
}