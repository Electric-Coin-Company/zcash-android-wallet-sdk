package cash.z.wallet.sdk.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.wallet.sdk.dao.CompactBlockDao
import cash.z.wallet.sdk.db.CompactBlockDb
import cash.z.wallet.sdk.ext.debug
import cash.z.wallet.sdk.jni.JniConverter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import cash.z.wallet.sdk.rpc.CompactFormats

/**
 * Downloads compact blocks to the database and then scans them for transactions
 */
    class Synchronizer(val applicationContext: Context, val scope: CoroutineScope, val birthday: Long = 373070L) {

    // TODO: convert to CompactBlockSource that just has a stream and then have the downloader operate on the stream
    private val downloader = CompactBlockDownloader("10.0.2.2", 9067)
    private val savedBlockChannel = ConflatedBroadcastChannel<CompactFormats.CompactBlock>()
    private lateinit var cacheDao: CompactBlockDao
    private lateinit var cacheDb: CompactBlockDb
    private lateinit var saveJob: Job
    private lateinit var scanJob: Job
    fun blocks(): ReceiveChannel<CompactFormats.CompactBlock> = savedBlockChannel.openSubscription()

    fun start() {
        createDb()
        downloader.start(scope, birthday)
        saveJob = saveBlocks()
        scanJob = scanBlocks()
    }

    fun stop() {
        scanJob.cancel()
        saveJob.cancel()
        downloader.stop()
        cacheDb.close()
    }

    private fun createDb() {
        // TODO: inject the db and dao
        cacheDb = Room.databaseBuilder(
            applicationContext,
            CompactBlockDb::class.java,
            CACHEDB_NAME
        )
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .fallbackToDestructiveMigration()
            .build()
            .apply { cacheDao = complactBlockDao() }
    }

    private fun saveBlocks(): Job = scope.launch {
//        val downloadedBlockChannel = downloader.blocks()
//        while (isActive) {
//            try {
//                val nextBlock = downloadedBlockChannel.receive()
//                cacheDao.insert(cash.z.wallet.sdk.vo.CompactBlock(nextBlock.height.toInt(), nextBlock.toByteArray()))
//                async {
//                    savedBlockChannel.send(Result.success(nextBlock))
//                    debug("stored block at height: ${nextBlock.height}")
//                }
//            } catch (t: Throwable) {
//                debug("failed to store block due to $t")
//                async {
//                    savedBlockChannel.send(Result.failure(t))
//                }
//            }
//
//        }
    }

    private fun scanBlocks(): Job = scope.launch {
        val savedBlocks = blocks()
        val converter = JniConverter()
        converter.initLogs()
        ScanResultDbCreator.create(applicationContext)
        while (isActive) {
            try {
                debug("scanning blocks from $birthday onward...")
                val nextBlock = savedBlocks.receive()
                debug("...scanner observed a block (${nextBlock.height}) without crashing!")
                delay(5000L)
                val result = converter.scanBlocks(
                    applicationContext.getDatabasePath(CACHEDB_NAME).absolutePath,
                    applicationContext.getDatabasePath(ScanResultDbCreator.DB_NAME).absolutePath,
                    "dummyseed".toByteArray(),
                    birthday.toInt()
                )
                debug("scan complete")
            } catch (t: Throwable) {
                debug("error while scanning blocks: $t")
            }

        }
    }

    companion object {
        const val CACHEDB_NAME = "DownloadedCompactBlocks.db"
    }
}