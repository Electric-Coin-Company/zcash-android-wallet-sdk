package cash.z.wallet.sdk.db

import androidx.room.*
import cash.z.wallet.sdk.entity.CompactBlock


//
// Database
//

@Database(
    entities = [
        CompactBlock::class],
    version = 1,
    exportSchema = false
)
abstract class CompactBlockDb : RoomDatabase() {
    abstract fun complactBlockDao(): CompactBlockDao
}


//
// Data Access Objects
//

@Dao
interface CompactBlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(block: CompactBlock)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(block: List<CompactBlock>)

    @Query("DELETE FROM compactblocks WHERE height >= :height")
    fun rewindTo(height: Int)

    @Query("SELECT MAX(height) FROM compactblocks")
    fun latestBlockHeight(): Int
}