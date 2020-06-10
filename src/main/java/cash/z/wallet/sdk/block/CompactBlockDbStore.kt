package cash.z.wallet.sdk.block

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.wallet.sdk.db.CompactBlockDao
import cash.z.wallet.sdk.db.CompactBlockDb
import cash.z.wallet.sdk.db.entity.CompactBlockEntity
import cash.z.wallet.sdk.ext.ZcashSdk.SAPLING_ACTIVATION_HEIGHT
import cash.z.wallet.sdk.rpc.CompactFormats
import kotlinx.coroutines.Dispatchers.IO
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

    override suspend fun getLatestHeight(): Int = withContext(IO) {
        val lastBlock = Math.max(0, cacheDao.latestBlockHeight())
        if (lastBlock < SAPLING_ACTIVATION_HEIGHT) -1 else lastBlock
    }

    override suspend fun findCompactBlock(height: Int): CompactFormats.CompactBlock? {
        return cacheDao.findCompactBlock(height)?.let { CompactFormats.CompactBlock.parseFrom(it) }
    }

    override suspend fun write(result: List<CompactFormats.CompactBlock>) = withContext(IO) {
        cacheDao.insert(result.map { CompactBlockEntity(it.height.toInt(), it.toByteArray()) })
    }

    override suspend fun rewindTo(height: Int) = withContext(IO) {
        cacheDao.rewindTo(height)
    }

    override fun close() {
        cacheDb.close()
    }
}
