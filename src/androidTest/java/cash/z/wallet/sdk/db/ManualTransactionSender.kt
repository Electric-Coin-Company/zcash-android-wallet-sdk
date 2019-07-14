package cash.z.wallet.sdk.db

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class ManualTransactionSender {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun sendTransactionViaTest() {
        val transaction = transactions.findById(12)
        val hex = transaction?.raw?.toHex()
        assertEquals("foo", hex)
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this)
            sb.append(String.format("%02x", b))
        return sb.toString()
    }

    companion object {
        private lateinit var transactions: TransactionDao
        private lateinit var db: DerivedDataDb

        @BeforeClass
        @JvmStatic
        fun setup() {
            // TODO: put this database in the assets directory and open it from there via .openHelperFactory(new AssetSQLiteOpenHelperFactory()) seen here https://github.com/albertogiunta/sqliteAsset
            db = Room
                .databaseBuilder(ApplicationProvider.getApplicationContext(), DerivedDataDb::class.java, "wallet_data1202.db")
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .fallbackToDestructiveMigration()
                .build()
                .apply {
                    transactions = transactionDao()
                }
        }

        @AfterClass
        @JvmStatic
        fun close() {
            db.close()
        }
    }
}
