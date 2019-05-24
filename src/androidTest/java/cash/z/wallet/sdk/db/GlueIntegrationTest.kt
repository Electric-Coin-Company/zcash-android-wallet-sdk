package cash.z.wallet.sdk.db

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import cash.z.wallet.sdk.dao.BlockDao
import cash.z.wallet.sdk.dao.CompactBlockDao
import cash.z.wallet.sdk.dao.TransactionDao
import cash.z.wallet.sdk.data.SampleSeedProvider
import cash.z.wallet.sdk.ext.toBlockHeight
import cash.z.wallet.sdk.jni.Librustzcash
import cash.z.wallet.sdk.entity.CompactBlock
import cash.z.wallet.sdk.jni.LibrustzcashWelding
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.junit.*
import org.junit.Assert.*
import cash.z.wallet.sdk.rpc.CompactTxStreamerGrpc
import cash.z.wallet.sdk.rpc.Service
import cash.z.wallet.sdk.rpc.Service.*
import java.util.concurrent.TimeUnit

class GlueIntegrationTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun testDbExists() {
        Log.e("tezt", "addData")
        addData()
        Log.e("tezt", "scanData")
        scanData()
        Log.e("tezt", "checkResults")
        checkResults()
    }

    private fun checkResults() {
        Thread.sleep(15000L)
    }

    private fun addData() {
        val result = blockingStub.getBlockRange(
            BlockRange.newBuilder()
                .setStart(373070.toBlockHeight())
                .setEnd(373085.toBlockHeight())
                .build()
        )
        while (result.hasNext()) {
            val compactBlock = result.next()
            dao.insert(CompactBlock(compactBlock.height.toInt(), compactBlock.toByteArray()))
            System.err.println("stored block at height: ${compactBlock.height} with time ${compactBlock.time}")
        }
    }

    private fun scanData() {
        val dbFileName = "/data/user/0/cash.z.wallet.sdk.test/databases/new-data-glue.db"
        librustzcash.initDataDb(dbFileName)
        librustzcash.initAccountsTable(dbFileName, "dummyseed".toByteArray(), 1)


        Log.e("tezt", "scanning blocks...")
        val result = librustzcash.scanBlocks(cacheDbPath, dbFileName)
        System.err.println("done.")
    }

    fun heightOf(height: Long): Service.BlockID {
        return BlockID.newBuilder().setHeight(height).build()
    }

    companion object {
        // jni
        val librustzcash: LibrustzcashWelding = Librustzcash()

        // db
        private lateinit var dao: CompactBlockDao
        private lateinit var db: CompactBlockDb
        private const val cacheDbName = "new-dummy-cache-glue.db"
        private const val cacheDbPath = "/data/user/0/cash.z.wallet.sdk.test/databases/$cacheDbName"

        // grpc
        lateinit var blockingStub: CompactTxStreamerGrpc.CompactTxStreamerBlockingStub

        @BeforeClass
        @JvmStatic
        fun setup() {
            librustzcash.initLogs()

            val channel = ManagedChannelBuilder.forAddress("10.0.2.2", 9067).usePlaintext().build()
            blockingStub = CompactTxStreamerGrpc.newBlockingStub(channel)

            db = Room
                .databaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    CompactBlockDb::class.java,
                    cacheDbName
                )
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .fallbackToDestructiveMigration()
                .build()
                .apply { dao = complactBlockDao() }
        }

        @AfterClass
        @JvmStatic
        fun close() {
            db.close()
            (blockingStub.channel as ManagedChannel).shutdown().awaitTermination(2000L, TimeUnit.MILLISECONDS)
        }
    }
}
