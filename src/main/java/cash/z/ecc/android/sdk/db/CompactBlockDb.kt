package cash.z.ecc.android.sdk.db

import androidx.room.*
import cash.z.ecc.android.sdk.db.entity.CompactBlockEntity


//
// Database
//

/**
 * The "Cache DB", serving as a cache of compact blocks, waiting to be processed. This will contain
 * the entire blockchain, from the birthdate of the wallet, forward. The [CompactBlockProcessor]
 * will copy blocks from this database, as they are scanned. In the future, those blocks can be
 * deleted because they are no longer needed. Currently, this efficiency has not been implemented.
 */
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

/**
 * Data access object for compact blocks in the "Cache DB."
 */
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

    @Query("SELECT data FROM compactblocks WHERE height = :height")
    fun findCompactBlock(height: Int): ByteArray?
}
