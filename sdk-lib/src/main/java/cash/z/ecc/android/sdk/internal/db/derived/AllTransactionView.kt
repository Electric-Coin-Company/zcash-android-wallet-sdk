package cash.z.ecc.android.sdk.internal.db.derived

import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.ecc.android.sdk.internal.db.CursorParser
import cash.z.ecc.android.sdk.internal.db.optBlobOrThrow
import cash.z.ecc.android.sdk.internal.db.queryAndMap
import cash.z.ecc.android.sdk.internal.model.DbTransactionOverview
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.Locale
import kotlin.math.absoluteValue

internal class AllTransactionView(
    private val zcashNetwork: ZcashNetwork,
    private val sqliteDatabase: SupportSQLiteDatabase
) {
    companion object {

        private const val COLUMN_SORT_HEIGHT = "sort_height" // $NON-NLS

        private const val QUERY_LIMIT = "1" // $NON-NLS

        private val COLUMNS = arrayOf(
            "*", // $NON-NLS
            @Suppress("MaxLineLength")
            "IFNULL(${AllTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT}, ${UInt.MAX_VALUE}) AS $COLUMN_SORT_HEIGHT" // $NON-NLS
        )

        private val ORDER_BY = String.format(
            Locale.ROOT,
            "%s DESC, %s DESC", // $NON-NLS
            COLUMN_SORT_HEIGHT,
            AllTransactionViewDefinition.COLUMN_INTEGER_ID
        )

        private val ORDER_BY_MINED_HEIGHT = String.format(
            Locale.ROOT,
            "%s ASC", // $NON-NLS
            AllTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT
        )

        private val SELECTION_BLOCK_RANGE = String.format(
            Locale.ROOT,
            "%s >= ? AND %s <= ?", // $NON-NLS
            AllTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT,
            AllTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT
        )

        private val SELECTION_RAW_IS_NULL = String.format(
            Locale.ROOT,
            "%s IS NULL", // $NON-NLS
            AllTransactionViewDefinition.COLUMN_BLOB_RAW
        )

        private val PROJECTION_COUNT = arrayOf("COUNT(*)") // $NON-NLS

        private val PROJECTION_MINED_HEIGHT = arrayOf(AllTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT)
    }

    private val cursorParser: CursorParser<DbTransactionOverview> = CursorParser { cursor ->
        val idColumnIndex = cursor.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_ID)
        val minedHeightColumnIndex =
            cursor.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT)
        val transactionIndexColumnIndex = cursor.getColumnIndex(
            AllTransactionViewDefinition.COLUMN_INTEGER_TRANSACTION_INDEX
        )
        val rawTransactionIdIndex =
            cursor.getColumnIndex(AllTransactionViewDefinition.COLUMN_BLOB_RAW_TRANSACTION_ID)
        val expiryHeightIndex = cursor.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_EXPIRY_HEIGHT)
        val rawIndex = cursor.getColumnIndex(AllTransactionViewDefinition.COLUMN_BLOB_RAW)
        val netValueIndex = cursor.getColumnIndex(AllTransactionViewDefinition.COLUMN_LONG_ACCOUNT_BALANCE_DELTA)
        val feePaidIndex = cursor.getColumnIndex(AllTransactionViewDefinition.COLUMN_LONG_FEE_PAID)
        val isChangeIndex = cursor.getColumnIndex(AllTransactionViewDefinition.COLUMN_BOOLEAN_IS_CHANGE)
        val receivedNoteCountIndex = cursor.getColumnIndex(
            AllTransactionViewDefinition.COLUMN_INTEGER_RECEIVED_NOTE_COUNT
        )
        val sentNoteCountIndex = cursor.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_SENT_NOTE_COUNT)
        val memoCountIndex = cursor.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_MEMO_COUNT)
        val blockTimeIndex = cursor.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_BLOCK_TIME)

        val netValueLong = cursor.getLong(netValueIndex)
        val isSent = netValueLong < 0

        val expiryHeightLong = cursor.getLong(expiryHeightIndex)

        DbTransactionOverview(
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
            isSentTransaction = isSent,
            netValue = Zatoshi(netValueLong.absoluteValue),
            feePaid = Zatoshi(cursor.getLong(feePaidIndex)),
            isChange = cursor.getInt(isChangeIndex) != 0,
            receivedNoteCount = cursor.getInt(receivedNoteCountIndex),
            sentNoteCount = cursor.getInt(sentNoteCountIndex),
            memoCount = cursor.getInt(memoCountIndex),
            blockTimeEpochSeconds = cursor.getLong(blockTimeIndex)
        )
    }

    suspend fun count() = sqliteDatabase.queryAndMap(
        AllTransactionViewDefinition.VIEW_NAME,
        columns = PROJECTION_COUNT,
        cursorParser = { it.getLong(0) }
    ).first()

    fun getAllTransactions() =
        sqliteDatabase.queryAndMap(
            table = AllTransactionViewDefinition.VIEW_NAME,
            columns = COLUMNS,
            orderBy = ORDER_BY,
            cursorParser = cursorParser
        )

    fun getTransactionRange(blockHeightRange: ClosedRange<BlockHeight>) =
        sqliteDatabase.queryAndMap(
            table = AllTransactionViewDefinition.VIEW_NAME,
            columns = COLUMNS,
            orderBy = ORDER_BY,
            selection = SELECTION_BLOCK_RANGE,
            selectionArgs = arrayOf(blockHeightRange.start.value, blockHeightRange.endInclusive.value),
            cursorParser = cursorParser
        )

    suspend fun getOldestTransaction() =
        sqliteDatabase.queryAndMap(
            table = AllTransactionViewDefinition.VIEW_NAME,
            columns = COLUMNS,
            orderBy = ORDER_BY,
            limit = QUERY_LIMIT,
            cursorParser = cursorParser
        ).firstOrNull()

    suspend fun firstUnenhancedHeight(): BlockHeight? {
        val heightLong =
            sqliteDatabase.queryAndMap(
                table = AllTransactionViewDefinition.VIEW_NAME,
                columns = PROJECTION_MINED_HEIGHT,
                orderBy = ORDER_BY_MINED_HEIGHT,
                selection = SELECTION_RAW_IS_NULL,
                limit = QUERY_LIMIT,
                cursorParser = { it.getLong(0) }
            ).firstOrNull()

        return if (heightLong != null) {
            BlockHeight.new(zcashNetwork, heightLong)
        } else {
            null
        }
    }
}

internal object AllTransactionViewDefinition {
    const val VIEW_NAME = "v_transactions" // $NON-NLS

    const val COLUMN_INTEGER_ID = "id_tx" // $NON-NLS

    const val COLUMN_INTEGER_MINED_HEIGHT = "mined_height" // $NON-NLS

    const val COLUMN_INTEGER_TRANSACTION_INDEX = "tx_index" // $NON-NLS

    const val COLUMN_BLOB_RAW_TRANSACTION_ID = "txid" // $NON-NLS

    const val COLUMN_INTEGER_EXPIRY_HEIGHT = "expiry_height" // $NON-NLS

    const val COLUMN_BLOB_RAW = "raw" // $NON-NLS

    const val COLUMN_LONG_ACCOUNT_BALANCE_DELTA = "account_balance_delta" // $NON-NLS

    const val COLUMN_LONG_FEE_PAID = "fee_paid" // $NON-NLS

    const val COLUMN_BOOLEAN_IS_CHANGE = "has_change" // $NON-NLS

    const val COLUMN_INTEGER_SENT_NOTE_COUNT = "sent_note_count" // $NON-NLS

    const val COLUMN_INTEGER_RECEIVED_NOTE_COUNT = "received_note_count" // $NON-NLS

    const val COLUMN_INTEGER_MEMO_COUNT = "memo_count" // $NON-NLS

    const val COLUMN_INTEGER_BLOCK_TIME = "block_time" // $NON-NLS
}
