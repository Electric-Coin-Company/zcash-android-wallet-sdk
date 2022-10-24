package cash.z.ecc.android.sdk.internal.db.derived

import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.ecc.android.sdk.internal.db.queryAndMap
import cash.z.ecc.android.sdk.model.ZcashNetwork
import java.util.Locale

internal class SentNoteTable(
    @Suppress("UnusedPrivateMember")
    private val zcashNetwork: ZcashNetwork,
    private val sqliteDatabase: SupportSQLiteDatabase
) {
    companion object {

        private val ORDER_BY = String.format(
            Locale.ROOT,
            "%s ASC", // $NON-NLS
            SentNoteTableDefinition.COLUMN_INTEGER_ID
        )

        private val PROJECTION_ID = arrayOf(SentNoteTableDefinition.COLUMN_INTEGER_ID)

        private val SELECT_BY_TRANSACTION_ID = String.format(
            Locale.ROOT,
            "%s = ?", // $NON-NLS
            SentNoteTableDefinition.COLUMN_INTEGER_TRANSACTION_ID
        )
    }

    fun getSentNoteIds(transactionId: Long) =
        sqliteDatabase.queryAndMap(
            table = SentNoteTableDefinition.TABLE_NAME,
            columns = PROJECTION_ID,
            selection = SELECT_BY_TRANSACTION_ID,
            selectionArgs = arrayOf(transactionId),
            orderBy = ORDER_BY,
            cursorParser = {
                val idColumnIndex = it.getColumnIndex(SentNoteTableDefinition.COLUMN_INTEGER_ID)

                it.getLong(idColumnIndex)
            }
        )
}

// https://github.com/zcash/librustzcash/blob/277d07c79c7a08907b05a6b29730b74cdb238b97/zcash_client_sqlite/src/wallet/init.rs#L393
internal object SentNoteTableDefinition {
    const val TABLE_NAME = "sent_notes" // $NON-NLS

    const val COLUMN_INTEGER_ID = "id_note" // $NON-NLS

    const val COLUMN_INTEGER_TRANSACTION_ID = "tx" // $NON-NLS

    const val COLUMN_INTEGER_OUTPUT_POOL = "output_pool" // $NON-NLS

    const val COLUMN_INTEGER_OUTPUT_INDEX = "output_index" // $NON-NLS

    const val COLUMN_INTEGER_FROM_ACCOUNT = "from_account" // $NON-NLS

    const val COLUMN_STRING_TO_ADDRESS = "to_address" // $NON-NLS

    const val COLUMN_INTEGER_TO_ACCOUNT = "to_account" // $NON-NLS

    const val COLUMN_INTEGER_VALUE = "value" // $NON-NLS

    const val COLUMN_BLOB_MEMO = "memo" // $NON-NLS
}
