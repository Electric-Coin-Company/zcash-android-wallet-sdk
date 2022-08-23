package cash.z.ecc.android.sdk.internal.db.derived

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import cash.z.ecc.android.sdk.internal.NoBackupContextWrapper
import cash.z.ecc.android.sdk.internal.db.ReadOnlySqliteOpenHelper
import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class DerivedDataDb private constructor(
    private val zcashNetwork: ZcashNetwork,
    private val sqliteDatabase:
        SQLiteDatabase
) {
    val blockTable = BlockTable(zcashNetwork, sqliteDatabase)

    val transactionTable = TransactionTable(zcashNetwork, sqliteDatabase)

    suspend fun close() {
        withContext(Dispatchers.IO) {
            sqliteDatabase.close()
        }
    }

    companion object {
        // Database migrations are managed by librustzcash.  This is a hard-coded value to ensure that Android's
        // SqliteOpenHelper is happy
        private const val DATABASE_VERSION = 8

        suspend fun new(
            context: Context,
            rustBackend: RustBackend,
            zcashNetwork: ZcashNetwork
        ): DerivedDataDb {
            rustBackend.initDataDb()

            // TODO: here we may need to make additional calls for:
            // rustBackend.initDataDb()
            // rustBackend.initBlocksTable(checkpoint)
            // rustBackend.initAccountsTable(*viewingKeys.toTypedArray())

            // TODO We also likely need to signal whether the Rust backend needs to be initialized with the spending key

            val database = ReadOnlySqliteOpenHelper.openExistingDatabaseAsReadOnly(
                NoBackupContextWrapper(
                    context,
                    rustBackend.dataDbFile.parentFile!!
                ),
                rustBackend.dataDbFile.name,
                DATABASE_VERSION
            )

            return DerivedDataDb(zcashNetwork, database)
        }
    }
}
