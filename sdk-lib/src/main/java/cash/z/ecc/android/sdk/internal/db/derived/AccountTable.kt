package cash.z.ecc.android.sdk.internal.db.derived

import android.database.sqlite.SQLiteDatabase
import cash.z.ecc.android.sdk.internal.db.queryAndMap
import cash.z.ecc.android.sdk.internal.model.Account
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.Locale

internal class AccountTable(private val sqliteDatabase: SQLiteDatabase) {
    companion object {

        private val SELECTION_ACCOUNT_ID = String.format(
            Locale.ROOT,
            "%s = ?", // $NON-NLS
            AccountTableDefinition.COLUMN_INTEGER_ID
        )

        private val PROJECTION_COUNT = arrayOf("COUNT(*)")
    }

    suspend fun count() = sqliteDatabase.queryAndMap(
        AccountTableDefinition.TABLE_NAME,
        columns = PROJECTION_COUNT,
        cursorParser = { it.getLong(0) }
    ).first()

    suspend fun getAccount(id: Int): Account? {
        return sqliteDatabase.queryAndMap(
            table = AccountTableDefinition.TABLE_NAME,
            selection = SELECTION_ACCOUNT_ID,
            selectionArgs = arrayOf(id.toString()),
            cursorParser = {
                val accountColumnIndex = it.getColumnIndex(AccountTableDefinition.COLUMN_INTEGER_ID)
                val extfvkColumnIndex = it.getColumnIndex(AccountTableDefinition.COLUMN_TEXT_UFVK)
                val addressColumnIndex = it.getColumnIndex(AccountTableDefinition.COLUMN_TEXT_ADDRESS)
                val transparentAddressColumnIndex = it.getColumnIndex(AccountTableDefinition.COLUMN_TEXT_TRANSPARENT_ADDRESS)

                Account(
                    account = it.getInt(accountColumnIndex),
                    extendedFullViewingKey = it.getString(extfvkColumnIndex),
                    address = it.getString(addressColumnIndex),
                    transparentAddress = it.getString(transparentAddressColumnIndex)
                )
            }
        ).firstOrNull()
    }
}

object AccountTableDefinition {
    const val TABLE_NAME = "accounts"

    const val COLUMN_INTEGER_ID = "account"

    const val COLUMN_TEXT_UFVK = "ufvk"

    const val COLUMN_TEXT_ADDRESS = "address"

    const val COLUMN_TEXT_TRANSPARENT_ADDRESS = "transparent_address"
}
