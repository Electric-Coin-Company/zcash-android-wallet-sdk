package cash.z.ecc.android.sdk.internal.block

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.ecc.android.sdk.internal.db.CompactBlockDao
import cash.z.ecc.android.sdk.internal.db.CompactBlockDb
import cash.z.ecc.android.sdk.db.entity.CompactBlockEntity
import cash.z.ecc.android.sdk.internal.SdkDispatchers
import cash.z.wallet.sdk.rpc.CompactFormats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An implementation of CompactBlockStore that persists information to a database in the given
 * path. This represents the "cache db" or local cache of compact blocks waiting to be scanned.
 *
 * @param appContext the application context. This is used for creating the database.
 * @property dbPath the absolute path to the database.
 */
class CompactBlockDbStore(
    appContext: Context,
    val dbPath: String
) : CompactBlockStore {

    private val cacheDao: CompactBlockDao
    private val cacheDb: CompactBlockDb

    init {
        cacheDb = createCompactBlockCacheDb(appContext)
        cacheDao = cacheDb.complactBlockDao()
    }

    private fun createCompactBlockCacheDb(appContext: Context): CompactBlockDb {
        return Room.databaseBuilder(appContext, CompactBlockDb::class.java, dbPath)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            // this is a simple cache of blocks. destroying the db should be benign
            .fallbackToDestructiveMigration()
            .build()
    }

    override suspend fun getLatestHeight(): Int = withContext(SdkDispatchers.IO) {
        Math.max(0, cacheDao.latestBlockHeight())
    }

    override suspend fun findCompactBlock(height: Int): CompactFormats.CompactBlock? {
        return cacheDao.findCompactBlock(height)?.let { CompactFormats.CompactBlock.parseFrom(it) }
    }

    override suspend fun write(result: List<CompactFormats.CompactBlock>) = withContext(SdkDispatchers.IO) {
        cacheDao.insert(result.map { CompactBlockEntity(it.height.toInt(), it.toByteArray()) })
    }

    override suspend fun rewindTo(height: Int) = withContext(SdkDispatchers.IO) {
        cacheDao.rewindTo(height)
    }

    override suspend fun close() {
        withContext(SdkDispatchers.IO) {
            cacheDb.close()
        }
    }
}
