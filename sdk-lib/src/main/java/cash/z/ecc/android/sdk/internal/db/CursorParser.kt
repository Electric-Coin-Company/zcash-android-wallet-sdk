package cash.z.ecc.android.sdk.internal.db

import android.database.Cursor

fun interface CursorParser<T> {
    /**
     * Extracts an object from a Cursor.  This method assumes that the Cursor contains all the needed columns and
     * that the Cursor is positioned to a row that is ready to be read. This method, in turn, will not mutate
     * the Cursor or move the Cursor position.
     *
     * @param cursor Cursor from a query to a contract this parser can handle.
     * @return a new Object.
     * @throws AssertionError If the cursor is closed or the cursor is out of range.
     */
    fun newObject(cursor: Cursor): T
}
