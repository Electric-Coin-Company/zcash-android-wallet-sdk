package cash.z.wallet.sdk.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.wallet.sdk.dao.CompactBlockDao
import cash.z.wallet.sdk.db.CompactBlockDb
import cash.z.wallet.sdk.exception.CompactBlockProcessorException
import cash.z.wallet.sdk.jni.JniConverter
import cash.z.wallet.sdk.rpc.CompactFormats
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty

/**
 * Responsible for processing the blocks on the stream. Saves them to the cacheDb and periodically scans for transactions.
 *
 * @property applicationContext used to connect to the DB on the device. No reference is kept beyond construction.
 */
class CompactBlockProcessor(
    applicationContext: Context,
    val converter: JniConverter = JniConverter(),
    cacheDbName: String = CACHE_DB_NAME,
    dataDbName: String = DATA_DB_NAME,
    logger: Twig = SilentTwig()
) : Twig by logger {

    internal val cacheDao: CompactBlockDao
    private val cacheDb: CompactBlockDb
    private val cacheDbPath: String
    private val dataDbPath: String

    val dataDbExists get() = File(dataDbPath).exists()
    val cachDbExists get() = File(cacheDbPath).exists()

    init {
        cacheDb = createCompactBlockCacheDb(applicationContext, cacheDbName)
        cacheDao = cacheDb.complactBlockDao()
        cacheDbPath = applicationContext.getDatabasePath(cacheDbName).absolutePath
        dataDbPath = applicationContext.getDatabasePath(dataDbName).absolutePath
    }

    private fun createCompactBlockCacheDb(applicationContext: Context, cacheDbName: String): CompactBlockDb {
        return Room.databaseBuilder(applicationContext, CompactBlockDb::class.java, cacheDbName)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            // this is a simple cache of blocks. destroying the db should be benign
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * Save blocks and periodically scan them.
     */
    suspend fun processBlocks(incomingBlocks: ReceiveChannel<CompactFormats.CompactBlock>) = withContext(IO) {
        ensureDataDb()
        twigTask("processing blocks") {
            var lastScanTime = System.currentTimeMillis()
            var hasScanned = false
            while (isActive && !incomingBlocks.isClosedForReceive) {
                twig("awaiting next block")
                val nextBlock = incomingBlocks.receive()
                val nextBlockHeight = nextBlock.height
                twig("received block with height ${nextBlockHeight} on thread ${Thread.currentThread().name}")
                cacheDao.insert(cash.z.wallet.sdk.entity.CompactBlock(nextBlockHeight.toInt(), nextBlock.toByteArray()))
                if (shouldScanBlocks(lastScanTime, hasScanned)) {
                    twig("last block prior to scan ${nextBlockHeight}")
                    scanBlocks()
                    lastScanTime = System.currentTimeMillis()
                    hasScanned = true
                }
            }
            cacheDb.close()
        }
    }

    private fun ensureDataDb() {
        if (!dataDbExists) throw CompactBlockProcessorException.DataDbMissing(dataDbPath)
    }

    private fun shouldScanBlocks(lastScanTime: Long, hasScanned: Boolean): Boolean {
        val deltaTime = System.currentTimeMillis() - lastScanTime
        twig("${deltaTime}ms since last scan. Have we ever scanned? $hasScanned")
        return (!hasScanned && deltaTime > INITIAL_SCAN_DELAY)
                || deltaTime > SCAN_FREQUENCY
    }

    suspend fun scanBlocks() = withContext(IO) {
        twigTask("scanning blocks") {
            if (isActive) {
                try {
                    converter.scanBlocks(cacheDbPath, dataDbPath)
                } catch (t: Throwable) {
                    twig("error while scanning blocks: $t")
                }
            }
        }
    }

    suspend fun lastProcessedBlock(): Int = withContext(IO) {
        // TODO: maybe start at the tip and keep going backward until we find a verifiably non-corrupted block, far enough back to be immune to reorgs
        Math.max(0, cacheDao.latestBlockHeight() - 20)
    }

    companion object {
        /** Default amount of time to synchronize before initiating the first scan. This allows time to download a few blocks. */
        const val INITIAL_SCAN_DELAY = 3000L
        /** Minimum amount of time between scans. The frequency with which we check whether the block height has changed and, if so, trigger a scan */
        const val SCAN_FREQUENCY = 75_000L
        const val CACHE_DB_NAME = "DownloadedCompactBlocks.db"
        const val DATA_DB_NAME = "CompactBlockScanResults.db"
    }
}
