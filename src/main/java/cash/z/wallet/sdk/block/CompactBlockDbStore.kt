package cash.z.wallet.sdk.block

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.wallet.sdk.db.CompactBlockDao
import cash.z.wallet.sdk.db.CompactBlockDb
import cash.z.wallet.sdk.entity.CompactBlockEntity
import cash.z.wallet.sdk.ext.ZcashSdk.DB_CACHE_NAME
import cash.z.wallet.sdk.ext.ZcashSdk.SAPLING_ACTIVATION_HEIGHT
import cash.z.wallet.sdk.rpc.CompactFormats
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class CompactBlockDbStore(
    applicationContext: Context,
    val dbPath: String
) : CompactBlockStore {

    private val cacheDao: CompactBlockDao
    private val cacheDb: CompactBlockDb

    init {
        cacheDb = createCompactBlockCacheDb(applicationContext)
        cacheDao = cacheDb.complactBlockDao()
    }

    private fun createCompactBlockCacheDb(applicationContext: Context): CompactBlockDb {
        return Room.databaseBuilder(applicationContext, CompactBlockDb::class.java, dbPath)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            // this is a simple cache of blocks. destroying the db should be benign
            .fallbackToDestructiveMigration()
            .build()
    }

    override suspend fun getLatestHeight(): Int = withContext(IO) {
        val lastBlock = Math.max(0, cacheDao.latestBlockHeight())
        if (lastBlock < SAPLING_ACTIVATION_HEIGHT) -1 else lastBlock
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