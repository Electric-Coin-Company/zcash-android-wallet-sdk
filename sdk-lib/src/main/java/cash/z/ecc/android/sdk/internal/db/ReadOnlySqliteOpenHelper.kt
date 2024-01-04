package cash.z.ecc.android.sdk.internal.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ReadOnlySqliteOpenHelper(
    context: Context,
    name: String,
    version: Int
) : SQLiteOpenHelper(context, name, null, version) {
    override fun onCreate(db: SQLiteDatabase?) {
        error("Database should be created by Rust libraries")
    }

    override fun onUpgrade(
        db: SQLiteDatabase?,
        oldVersion: Int,
        newVersion: Int
    ) {
        error("Database should be upgraded by Rust libraries")
    }

    internal companion object {
        /**
         * Opens a database that has already been initialized by something else.
         *
         * @param context Application context.
         * @param name Database file name.
         * @param databaseVersion Version of the database as set in https://sqlite.org/pragma.html#pragma_user_version
         * This is required to bypass database creation/migration logic in Android.
         */
        suspend fun openExistingDatabaseAsReadOnly(
            context: Context,
            name: String,
            databaseVersion: Int
        ): SQLiteDatabase {
            return withContext(Dispatchers.IO) {
                ReadOnlySqliteOpenHelper(
                    context,
                    name,
                    databaseVersion
                ).readableDatabase
            }
        }
    }
}
