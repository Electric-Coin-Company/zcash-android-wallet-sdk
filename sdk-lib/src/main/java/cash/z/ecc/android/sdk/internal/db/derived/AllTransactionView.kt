package cash.z.ecc.android.sdk.internal.db.derived

import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.ecc.android.sdk.internal.db.CursorParser
import cash.z.ecc.android.sdk.internal.db.queryAndMap
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.Transaction
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.flow.first
import java.util.Locale

internal class AllTransactionView(
    private val zcashNetwork: ZcashNetwork,
    private val sqliteDatabase: SupportSQLiteDatabase
) {
    companion object {

        private val ORDER_BY = String.format(
            Locale.ROOT,
            "%s DESC, %s DESC",
            AllTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT,
            AllTransactionViewDefinition.COLUMN_INTEGER_ID
        )

        private val SELECTION_BLOCK_RANGE = String.format(
            Locale.ROOT,
            "%s >= ? AND %s <= ?", // $NON-NLS
            AllTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT,
            AllTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT
        )

        private val PROJECTION_COUNT = arrayOf("COUNT(*)")
    }

    private val cursorParser: CursorParser<Transaction> = CursorParser {
        val idColumnIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_ID)
        val minedHeightColumnIndex =
            it.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT)
        val transactionIndexColumnIndex = it.getColumnIndex(
            AllTransactionViewDefinition
                .COLUMN_INTEGER_TRANSACTION_INDEX
        )
        val rawTransactionIdIndex =
            it.getColumnIndex(AllTransactionViewDefinition.COLUMN_BLOB_RAW_TRANSACTION_ID)
        val expiryHeightIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_EXPIRY_HEIGHT)
        val rawIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_BLOB_RAW)
        val valueIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_VALUE)
        val noteCountIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_INT_NOTE_COUNT)
        val memoCountIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_MEMO_COUNT)
        // val blockTimeIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_BLOCK_TIME)

        val value = it.getLong(valueIndex)
        if (value < 0) {
            Transaction.Sent(
                id = it.getLong(idColumnIndex),
                rawId = FirstClassByteArray(it.getBlob(rawTransactionIdIndex)),
                minedHeight = BlockHeight.new(zcashNetwork, it.getLong(minedHeightColumnIndex)),
                expiryHeight = BlockHeight.new(zcashNetwork, it.getLong(expiryHeightIndex)),
                index = it.getLong(transactionIndexColumnIndex),
                raw = FirstClassByteArray(it.getBlob(rawIndex)),
                sentTotal = Zatoshi(-value),
                sentNoteCount = it.getInt(noteCountIndex),
                memoCount = it.getInt(memoCountIndex),
                time = 0 // FIXME it.getLong(blockTimeIndex)
            )
        } else {
            Transaction.Received(
                id = it.getLong(idColumnIndex),
                rawId = FirstClassByteArray(it.getBlob(rawTransactionIdIndex)),
                minedHeight = BlockHeight.new(zcashNetwork, it.getLong(minedHeightColumnIndex)),
                index = it.getLong(transactionIndexColumnIndex),
                raw = FirstClassByteArray(it.getBlob(rawIndex)),
                receivedTotal = Zatoshi(it.getLong(valueIndex)),
                receivedNoteCount = it.getInt(noteCountIndex),
                memoCount = it.getInt(memoCountIndex),
                time = 0 // FIXME it.getLong(blockTimeIndex)
            )
        }
    }

    suspend fun count() = sqliteDatabase.queryAndMap(
        AllTransactionViewDefinition.VIEW_NAME,
        columns = PROJECTION_COUNT,
        cursorParser = { it.getLong(0) }
    ).first()

    fun getAllTransactions() =
        sqliteDatabase.queryAndMap(
            table = AllTransactionViewDefinition.VIEW_NAME,
            orderBy = ORDER_BY,
            cursorParser = cursorParser
        )

    fun getTransactionRange(blockHeightRange: ClosedRange<BlockHeight>) =
        sqliteDatabase.queryAndMap(
            table = AllTransactionViewDefinition.VIEW_NAME,
            orderBy = ORDER_BY,
            selection = SELECTION_BLOCK_RANGE,
            selectionArgs = arrayOf(blockHeightRange.start.value, blockHeightRange.endInclusive.value),
            cursorParser = cursorParser
        )
}

internal object AllTransactionViewDefinition {
    const val VIEW_NAME = "v_transactions"

    const val COLUMN_INTEGER_ID = "id_tx"

    const val COLUMN_INTEGER_MINED_HEIGHT = "mined_height"

    const val COLUMN_INTEGER_TRANSACTION_INDEX = "tx_index"

    const val COLUMN_BLOB_RAW_TRANSACTION_ID = "txid"

    const val COLUMN_INTEGER_EXPIRY_HEIGHT = "expiry_height"

    const val COLUMN_BLOB_RAW = "raw"

    const val COLUMN_INTEGER_VALUE = "net_value"

    const val COLUMN_BOOLEAN_IS_CHANGE = "has_change"

    const val COLUMN_INT_NOTE_COUNT = "note_count"

    const val COLUMN_INTEGER_MEMO_COUNT = "memo_count"

    const val COLUMN_INTEGER_BLOCK_TIME = "block_time"
}
