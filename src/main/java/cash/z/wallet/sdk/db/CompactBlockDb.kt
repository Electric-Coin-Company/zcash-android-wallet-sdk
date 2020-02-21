package cash.z.wallet.sdk.db

import androidx.room.*
import cash.z.wallet.sdk.entity.CompactBlockEntity


//
// Database
//

@Database(
    entities = [CompactBlockEntity::class],
    version = 1,
    exportSchema = true
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
    fun insert(block: CompactBlockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(block: List<CompactBlockEntity>)

    @Query("DELETE FROM compactblocks WHERE height >= :height")
    fun rewindTo(height: Int)

    @Query("SELECT MAX(height) FROM compactblocks")
    fun latestBlockHeight(): Int
}