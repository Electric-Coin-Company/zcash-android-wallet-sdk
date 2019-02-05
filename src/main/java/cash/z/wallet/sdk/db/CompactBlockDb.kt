package cash.z.wallet.sdk.db

import androidx.room.Database
import androidx.room.RoomDatabase
import cash.z.wallet.sdk.dao.CompactBlockDao
import cash.z.wallet.sdk.entity.CompactBlock

@Database(
    entities = [
        CompactBlock::class],
    version = 1,
    exportSchema = false
)
abstract class CompactBlockDb : RoomDatabase() {
    abstract fun complactBlockDao(): CompactBlockDao
}