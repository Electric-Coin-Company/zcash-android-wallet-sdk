package cash.z.ecc.android.sdk.internal.db.derived

import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.ecc.android.sdk.internal.db.queryAndMap
import kotlinx.coroutines.flow.first

internal class AccountTable(private val sqliteDatabase: SupportSQLiteDatabase) {
    companion object {

        private val PROJECTION_COUNT = arrayOf("COUNT(*)") // $NON-NLS
    }

    suspend fun count() = sqliteDatabase.queryAndMap(
        AccountTableDefinition.TABLE_NAME,
        columns = PROJECTION_COUNT,
        cursorParser = { it.getLong(0) }
    ).first()
}

object AccountTableDefinition {
    const val TABLE_NAME = "accounts" // $NON-NLS
}
