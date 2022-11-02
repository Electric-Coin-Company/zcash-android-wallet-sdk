@file:Suppress("ktlint:filename")

package cash.z.ecc.android.sdk.internal.db

import android.database.Cursor

internal fun Cursor.optLong(columnIndex: Int): Long? =
    if (isNull(columnIndex)) {
        null
    } else {
        getLong(columnIndex)
    }

internal fun Cursor.optBlobOrThrow(index: Int): ByteArray? {
    return if (isNull(index)) {
        null
    } else {
        getBlob(index)
    }
}
