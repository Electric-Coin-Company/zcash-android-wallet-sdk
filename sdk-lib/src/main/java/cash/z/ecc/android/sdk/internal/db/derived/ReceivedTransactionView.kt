package cash.z.ecc.android.sdk.internal.db.derived

import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.ecc.android.sdk.internal.db.optBlobOrThrow
import cash.z.ecc.android.sdk.internal.db.queryAndMap
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.Transaction
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.flow.first
import java.util.Locale

internal class ReceivedTransactionView(
    private val zcashNetwork: ZcashNetwork,
    private val sqliteDatabase: SupportSQLiteDatabase
) {
    companion object {

        private const val COLUMN_SORT_HEIGHT = "sort_height"

        private val COLUMNS = arrayOf(
            "*", // $NON-NLS
            @Suppress("MaxLineLength")
            "IFNULL(${ReceivedTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT}, ${UInt.MAX_VALUE}) AS $COLUMN_SORT_HEIGHT" // $NON-NLS
        )

        private val ORDER_BY = String.format(
            Locale.ROOT,
            "%s DESC, %s DESC", // $NON-NLS
            COLUMN_SORT_HEIGHT,
            ReceivedTransactionViewDefinition.COLUMN_INTEGER_ID
        )

        private val PROJECTION_COUNT = arrayOf("COUNT(*)") // $NON-NLS
    }

    suspend fun count() = sqliteDatabase.queryAndMap(
        ReceivedTransactionViewDefinition.VIEW_NAME,
        columns = PROJECTION_COUNT,
        cursorParser = { it.getLong(0) }
    ).first()

    fun getReceivedTransactions() =
        sqliteDatabase.queryAndMap(
            table = ReceivedTransactionViewDefinition.VIEW_NAME,
            columns = COLUMNS,
            orderBy = ORDER_BY,
            cursorParser = { cursor ->
                val idColumnIndex = cursor.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_INTEGER_ID)
                val minedHeightColumnIndex =
                    cursor.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT)
                val transactionIndexColumnIndex = cursor.getColumnIndex(
                    ReceivedTransactionViewDefinition
                        .COLUMN_INTEGER_TRANSACTION_INDEX
                )
                val rawTransactionIdIndex =
                    cursor.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_BLOB_RAW_TRANSACTION_ID)
                val expiryHeightIndex = cursor.getColumnIndex(
                    ReceivedTransactionViewDefinition.COLUMN_INTEGER_EXPIRY_HEIGHT
                )
                val rawIndex = cursor.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_BLOB_RAW)
                val receivedAccountIndex = cursor.getColumnIndex(
                    ReceivedTransactionViewDefinition.COLUMN_INTEGER_RECEIVED_BY_ACCOUNT
                )
                val receivedTotalIndex =
                    cursor.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_INTEGER_RECEIVED_TOTAL)
                val receivedNoteCountIndex =
                    cursor.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_INTEGER_RECEIVED_NOTE_COUNT)
                val memoCountIndex = cursor.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_INTEGER_MEMO_COUNT)
                val blockTimeIndex = cursor.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_INTEGER_BLOCK_TIME)

                val expiryHeightLong = cursor.getLong(expiryHeightIndex)

                Transaction.Received(
                    id = cursor.getLong(idColumnIndex),
                    rawId = FirstClassByteArray(cursor.getBlob(rawTransactionIdIndex)),
                    minedHeight = BlockHeight.new(zcashNetwork, cursor.getLong(minedHeightColumnIndex)),
                    expiryHeight = if (0L == expiryHeightLong) {
                        null
                    } else {
                        BlockHeight.new(zcashNetwork, expiryHeightLong)
                    },
                    index = cursor.getLong(transactionIndexColumnIndex),
                    raw = cursor.optBlobOrThrow(rawIndex)?.let { FirstClassByteArray(it) },
                    receivedByAccount = Account(cursor.getInt(receivedAccountIndex)),
                    receivedTotal = Zatoshi(cursor.getLong(receivedTotalIndex)),
                    receivedNoteCount = cursor.getInt(receivedNoteCountIndex),
                    memoCount = cursor.getInt(memoCountIndex),
                    blockTimeEpochSeconds = cursor.getLong(blockTimeIndex)
                )
            }
        )
}

internal object ReceivedTransactionViewDefinition {
    const val VIEW_NAME = "v_tx_received" // $NON-NLS

    const val COLUMN_INTEGER_ID = "id_tx" // $NON-NLS

    const val COLUMN_INTEGER_MINED_HEIGHT = "mined_height" // $NON-NLS

    const val COLUMN_INTEGER_TRANSACTION_INDEX = "tx_index" // $NON-NLS

    const val COLUMN_BLOB_RAW_TRANSACTION_ID = "txid" // $NON-NLS

    const val COLUMN_INTEGER_EXPIRY_HEIGHT = "expiry_height" // $NON-NLS

    const val COLUMN_BLOB_RAW = "raw" // $NON-NLS

    const val COLUMN_INTEGER_RECEIVED_BY_ACCOUNT = "received_by_account" // $NON-NLS

    const val COLUMN_INTEGER_RECEIVED_TOTAL = "received_total" // $NON-NLS

    const val COLUMN_INTEGER_RECEIVED_NOTE_COUNT = "received_note_count" // $NON-NLS

    const val COLUMN_INTEGER_MEMO_COUNT = "memo_count" // $NON-NLS

    const val COLUMN_INTEGER_BLOCK_TIME = "block_time" // $NON-NLS
}
