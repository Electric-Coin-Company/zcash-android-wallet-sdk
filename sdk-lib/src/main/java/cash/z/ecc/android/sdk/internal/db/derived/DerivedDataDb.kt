package cash.z.ecc.android.sdk.internal.db.derived

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.ecc.android.sdk.internal.NoBackupContextWrapper
import cash.z.ecc.android.sdk.internal.SdkDispatchers
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.TypesafeBackend
import cash.z.ecc.android.sdk.internal.db.ReadOnlySupportSqliteOpenHelper
import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.model.AccountSetup
import cash.z.ecc.android.sdk.model.BlockHeight
import kotlinx.coroutines.withContext
import java.io.File

internal class DerivedDataDb private constructor(
    private val sqliteDatabase: SupportSQLiteDatabase
) {
    val transactionTable = TransactionTable(sqliteDatabase)

    val allTransactionView = AllTransactionView(sqliteDatabase)

    val txOutputsView = TxOutputsView(sqliteDatabase)

    suspend fun close() =
        withContext(SdkDispatchers.DATABASE_IO) {
            sqliteDatabase.close()
        }

    suspend fun isClosed(): Boolean =
        withContext(SdkDispatchers.DATABASE_IO) {
            !sqliteDatabase.isOpen
        }

    companion object {
        // Database migrations are managed by librustzcash.  This is a hard-coded value to ensure that Android's
        // SqliteOpenHelper is happy
        private const val DATABASE_VERSION = 8

        @Suppress("LongParameterList")
        suspend fun new(
            context: Context,
            backend: TypesafeBackend,
            databaseFile: File,
            checkpoint: Checkpoint,
            numberOfAccounts: Int,
            recoverUntil: BlockHeight?,
            setup: AccountSetup?,
        ): DerivedDataDb {
            backend.initDataDb(setup?.seed)

            val database =
                ReadOnlySupportSqliteOpenHelper.openExistingDatabaseAsReadOnly(
                    NoBackupContextWrapper(
                        context,
                        databaseFile.parentFile!!
                    ),
                    databaseFile,
                    DATABASE_VERSION
                )

            val dataDb = DerivedDataDb(database)

            // If a seed is provided, fill in the accounts.
            setup?.let { checkedSetup ->
                val missingAccounts = numberOfAccounts - backend.getAccounts().count()
                require(missingAccounts >= 0) {
                    "Unexpected number of accounts: $missingAccounts"
                }
                repeat(missingAccounts) {
                    runCatching {
                        backend.createAccountAndGetSpendingKey(
                            accountName = checkedSetup.accountName,
                            keySource = checkedSetup.keySource,
                            recoverUntil = recoverUntil,
                            seed = checkedSetup.seed,
                            treeState = checkpoint.treeState(),
                        )
                    }.onFailure {
                        Twig.error(it) { "Create account failed." }
                    }
                }
            }

            return dataDb
        }
    }
}
