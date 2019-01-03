package cash.z.wallet.sdk.db

import androidx.room.Database
import androidx.room.RoomDatabase
import cash.z.wallet.sdk.dao.BlockDao
import cash.z.wallet.sdk.dao.NoteDao
import cash.z.wallet.sdk.dao.TransactionDao
import cash.z.wallet.sdk.vo.Block
import cash.z.wallet.sdk.vo.Note
import cash.z.wallet.sdk.vo.Transaction

@Database(
    entities = [
        Transaction::class,
        Block::class,
        Note::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DerivedDataDb : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun noteDao(): NoteDao
    abstract fun blockDao(): BlockDao
}