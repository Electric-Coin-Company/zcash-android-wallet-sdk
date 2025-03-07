package cash.z.ecc.android.sdk.internal.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import cash.z.ecc.android.sdk.exception.InitializeException
import cash.z.ecc.android.sdk.internal.NoBackupContextWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ReadOnlySupportSqliteOpenHelper {
    /**
     * Opens a database that has already been initialized by something else.
     *
     * @param context Application context.
     * @param file Database file name.
     * @param databaseVersion Version of the database as set in https://sqlite.org/pragma.html#pragma_user_version
     * This is required to bypass database creation/migration logic in Android.
     */
    suspend fun openExistingDatabaseAsReadOnly(
        context: Context,
        file: File,
        databaseVersion: Int
    ): SupportSQLiteDatabase =
        withContext(Dispatchers.IO) {
            val contextWrapper =
                NoBackupContextWrapper(
                    context,
                    file.parentFile ?: throw InitializeException.DatabasePathException
                )
            val config =
                SupportSQLiteOpenHelper.Configuration
                    .builder(contextWrapper)
                    .apply {
                        name(file.name)
                        callback(ReadOnlyCallback(databaseVersion))
                    }.build()

            FrameworkSQLiteOpenHelperFactory().create(config).readableDatabase
        }
}

private class ReadOnlyCallback(
    version: Int
) : SupportSQLiteOpenHelper.Callback(version) {
    override fun onCreate(db: SupportSQLiteDatabase) {
        error("Database ${db.path} should be created by Rust libraries")
    }

    override fun onUpgrade(
        db: SupportSQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        error("Database ${db.path} should be upgraded by Rust libraries")
    }
}
