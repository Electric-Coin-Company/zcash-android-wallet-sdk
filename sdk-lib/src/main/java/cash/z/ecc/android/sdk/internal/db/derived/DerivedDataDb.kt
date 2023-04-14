package cash.z.ecc.android.sdk.internal.db.derived

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.ecc.android.sdk.internal.Backend
import cash.z.ecc.android.sdk.internal.NoBackupContextWrapper
import cash.z.ecc.android.sdk.internal.db.ReadOnlySupportSqliteOpenHelper
import cash.z.ecc.android.sdk.internal.ext.tryWarn
import cash.z.ecc.android.sdk.internal.initAccountsTable
import cash.z.ecc.android.sdk.internal.initBlocksTable
import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.model.UnifiedFullViewingKey
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class DerivedDataDb private constructor(
    zcashNetwork: ZcashNetwork,
    private val sqliteDatabase: SupportSQLiteDatabase
) {
    val accountTable = AccountTable(sqliteDatabase)

    val blockTable = BlockTable(zcashNetwork, sqliteDatabase)

    val transactionTable = TransactionTable(zcashNetwork, sqliteDatabase)

    val allTransactionView = AllTransactionView(zcashNetwork, sqliteDatabase)

    val txOutputsView = TxOutputsView(zcashNetwork, sqliteDatabase)

    suspend fun close() {
        withContext(Dispatchers.IO) {
            sqliteDatabase.close()
        }
    }

    companion object {
        // Database migrations are managed by librustzcash.  This is a hard-coded value to ensure that Android's
        // SqliteOpenHelper is happy
        private const val DATABASE_VERSION = 8

        @Suppress("LongParameterList", "SpreadOperator")
        suspend fun new(
            context: Context,
            backend: Backend,
            databaseFile: File,
            zcashNetwork: ZcashNetwork,
            checkpoint: Checkpoint,
            seed: ByteArray?,
            viewingKeys: List<UnifiedFullViewingKey>
        ): DerivedDataDb {
            backend.initDataDb(seed)

            // TODO [#681]: consider converting these to typed exceptions in the welding layer
            // TODO [#681]: https://github.com/zcash/zcash-android-wallet-sdk/issues/681
            tryWarn(
                "Did not initialize the blocks table. It probably was already initialized.",
                ifContains = "table is not empty"
            ) {
                backend.initBlocksTable(checkpoint)
            }

            tryWarn(
                "Did not initialize the accounts table. It probably was already initialized.",
                ifContains = "table is not empty"
            ) {
                backend.initAccountsTable(*viewingKeys.toTypedArray())
            }

            val database = ReadOnlySupportSqliteOpenHelper.openExistingDatabaseAsReadOnly(
                NoBackupContextWrapper(
                    context,
                    databaseFile.parentFile!!
                ),
                databaseFile,
                DATABASE_VERSION
            )

            return DerivedDataDb(zcashNetwork, database)
        }
    }
}
