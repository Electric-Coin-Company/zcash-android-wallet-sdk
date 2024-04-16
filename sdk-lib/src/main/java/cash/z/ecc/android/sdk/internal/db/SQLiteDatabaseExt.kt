@file:Suppress("ktlint:standard:filename")

package cash.z.ecc.android.sdk.internal.db

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import cash.z.ecc.android.sdk.internal.SdkDispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Locale

/**
 * Performs a query on a background thread using the dedicated [SdkDispatchers.DATABASE_IO] dispatcher.
 *
 * Note that this method is best for small queries, as Cursor has an in-memory window of cached data.  If iterating
 * through a large number of items that exceeds the window, the Cursor may perform additional IO.
 */
@Suppress("LongParameterList")
internal fun <T> SQLiteDatabase.queryAndMap(
    table: String,
    columns: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<Any>? = null,
    groupBy: String? = null,
    having: String? = null,
    orderBy: String? = null,
    limit: String? = null,
    offset: String? = null,
    cursorParser: CursorParser<T>
) = flow<T> {
    // TODO [#703]: Support blobs for argument binding
    // TODO [#703]: https://github.com/zcash/zcash-android-wallet-sdk/issues/703
    val mappedSelectionArgs =
        selectionArgs?.onEach {
            require(it !is ByteArray) {
                "ByteArray is not supported"
            }
        }?.map { it.toString() }?.toTypedArray()

    // Counterintuitive but correct. When using the comma syntax, offset comes first.
    // When using the keyword syntax, "LIMIT 1 OFFSET 2" then the offset comes second.
    val limitAndOffset =
        if (null == offset) {
            limit
        } else {
            String.format(Locale.ROOT, "%s,%s", offset, limit) // NON-NLS
        }

    query(
        table,
        columns,
        selection,
        mappedSelectionArgs,
        groupBy,
        having,
        orderBy,
        limitAndOffset
    ).use {
        it.moveToPosition(-1)
        while (it.moveToNext()) {
            emit(cursorParser.newObject(it))
        }
    }
}.flowOn(SdkDispatchers.DATABASE_IO)

/**
 * Performs a query on a background thread using the dedicated [SdkDispatchers.DATABASE_IO] dispatcher.
 *
 * Note that this method is best for small queries, as Cursor has an in-memory window of cached data.  If iterating
 * through a large number of items that exceeds the window, the Cursor may perform additional IO.
 */
@Suppress("LongParameterList")
internal fun <T> SupportSQLiteDatabase.queryAndMap(
    table: String,
    columns: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<Any>? = null,
    groupBy: String? = null,
    having: String? = null,
    orderBy: String? = null,
    limit: String? = null,
    offset: String? = null,
    cursorParser: CursorParser<T>
) = flow<T> {
    val qb =
        SupportSQLiteQueryBuilder.builder(table).apply {
            columns(columns)
            selection(selection, selectionArgs)
            having(having)
            groupBy(groupBy)
            orderBy(orderBy)

            if (null != limit) {
                // Counterintuitive but correct. When using the comma syntax, offset comes first.
                // When using the keyword syntax, "LIMIT 1 OFFSET 2" then the offset comes second.
                if (null == offset) {
                    limit(limit)
                } else {
                    limit(String.format(Locale.ROOT, "%s,%s", offset, limit)) // NON-NLS
                }
            }
        }

    query(qb.create()).use {
        it.moveToPosition(-1)
        while (it.moveToNext()) {
            emit(cursorParser.newObject(it))
        }
    }
}.flowOn(SdkDispatchers.DATABASE_IO)
