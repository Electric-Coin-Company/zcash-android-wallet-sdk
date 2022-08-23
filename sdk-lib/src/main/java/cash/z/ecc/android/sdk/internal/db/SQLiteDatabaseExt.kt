@file:Suppress("ktlint:filename")

package cash.z.ecc.android.sdk.internal.db

import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Performs a query on a background thread.
 *
 * Note that this method is best for small queries, as Cursor has an in-memory window of cached data.  If iterating
 * through a large number of items that exceeds the window, the Cursor may perform additional IO.
 */
@Suppress("LongParameterList")
internal fun <T> SQLiteDatabase.queryAndMap(
    table: String,
    columns: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    groupBy: String? = null,
    having: String? = null,
    orderBy: String? = null,
    limit: String? = null,
    cursorParser: CursorParser<T>
) = flow<T> {
    query(
        table,
        columns,
        selection,
        selectionArgs,
        groupBy,
        having,
        orderBy,
        limit
    ).use {
        it.moveToPosition(-1)
        while (it.moveToNext()) {
            emit(cursorParser.newObject(it))
        }
    }
}.flowOn(Dispatchers.IO)
