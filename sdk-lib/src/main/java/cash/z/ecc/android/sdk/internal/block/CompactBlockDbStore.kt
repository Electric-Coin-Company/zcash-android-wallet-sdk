package cash.z.ecc.android.sdk.internal.block

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.ecc.android.sdk.db.entity.CompactBlockEntity
import cash.z.ecc.android.sdk.internal.SdkDispatchers
import cash.z.ecc.android.sdk.internal.SdkExecutors
import cash.z.ecc.android.sdk.internal.db.CompactBlockDb
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.wallet.sdk.rpc.CompactFormats
import kotlinx.coroutines.withContext

/**
 * An implementation of CompactBlockStore that persists information to a database in the given
 * path. This represents the "cache db" or local cache of compact blocks waiting to be scanned.
 */
class CompactBlockDbStore private constructor(
    private val network: ZcashNetwork,
    private val cacheDb: CompactBlockDb
) : CompactBlockStore {

    private val cacheDao = cacheDb.compactBlockDao()

    override suspend fun getLatestHeight(): BlockHeight? = runCatching {
        BlockHeight.new(network, cacheDao.latestBlockHeight())
    }.getOrNull()

    override suspend fun findCompactBlock(height: BlockHeight): CompactFormats.CompactBlock? =
        cacheDao.findCompactBlock(height.value)?.let { CompactFormats.CompactBlock.parseFrom(it) }

    override suspend fun write(result: Sequence<CompactFormats.CompactBlock>) =
        cacheDao.insert(result.map { CompactBlockEntity(it.height, it.toByteArray()) })

    override suspend fun rewindTo(height: BlockHeight) =
        cacheDao.rewindTo(height.value)

    override suspend fun close() {
        withContext(SdkDispatchers.DATABASE_IO) {
            cacheDb.close()
        }
    }

    companion object {
        /**
         * @param appContext the application context. This is used for creating the database.
         * @property dbPath the absolute path to the database.
         */
        fun new(
            appContext: Context,
            zcashNetwork: ZcashNetwork,
            dbPath: String
        ): CompactBlockDbStore {
            val cacheDb = createCompactBlockCacheDb(appContext.applicationContext, dbPath)

            return CompactBlockDbStore(zcashNetwork, cacheDb)
        }

        private fun createCompactBlockCacheDb(
            appContext: Context,
            dbPath: String
        ): CompactBlockDb {
            return Room.databaseBuilder(appContext, CompactBlockDb::class.java, dbPath)
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                // this is a simple cache of blocks. destroying the db should be benign
                .fallbackToDestructiveMigration()
                .setQueryExecutor(SdkExecutors.DATABASE_IO)
                .setTransactionExecutor(SdkExecutors.DATABASE_IO)
                .build()
        }
    }
}
