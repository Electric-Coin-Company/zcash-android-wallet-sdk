package cash.z.ecc.android.sdk.internal.db.derived

import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.ecc.android.sdk.internal.db.CursorParser
import cash.z.ecc.android.sdk.internal.db.queryAndMap
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.flow.first
import java.util.Locale
import kotlin.math.absoluteValue

internal class AllTransactionView(
    private val zcashNetwork: ZcashNetwork,
    private val sqliteDatabase: SupportSQLiteDatabase
) {
    companion object {

        private val ORDER_BY = String.format(
            Locale.ROOT,
            "%s DESC, %s DESC", // $NON-NLS
            AllTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT,
            AllTransactionViewDefinition.COLUMN_INTEGER_ID
        )

        private val SELECTION_BLOCK_RANGE = String.format(
            Locale.ROOT,
            "%s >= ? AND %s <= ?", // $NON-NLS
            AllTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT,
            AllTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT
        )

        private val PROJECTION_COUNT = arrayOf("COUNT(*)") // $NON-NLS
    }

    private val cursorParser: CursorParser<TransactionOverview> = CursorParser {
        val idColumnIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_ID)
        val minedHeightColumnIndex =
            it.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT)
        val transactionIndexColumnIndex = it.getColumnIndex(
            AllTransactionViewDefinition.COLUMN_INTEGER_TRANSACTION_INDEX
        )
        val rawTransactionIdIndex =
            it.getColumnIndex(AllTransactionViewDefinition.COLUMN_BLOB_RAW_TRANSACTION_ID)
        val expiryHeightIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_EXPIRY_HEIGHT)
        val rawIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_BLOB_RAW)
        val netValueIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_LONG_VALUE)
        val feePaidIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_LONG_FEE_PAID)
        val isChangeIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_BOOLEAN_IS_CHANGE)
        val isWalletInternalIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_BOOLEAN_IS_WALLET_INTERNAL)
        val receivedNoteCountIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_RECEIVED_NOTE_COUNT)
        val sentNoteCountIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_SENT_NOTE_COUNT)
        val memoCountIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_MEMO_COUNT)
        val blockTimeIndex = it.getColumnIndex(AllTransactionViewDefinition.COLUMN_INTEGER_BLOCK_TIME)

        val netValueLong = it.getLong(netValueIndex)
        val isSent = netValueLong < 0

        TransactionOverview(
            id = it.getLong(idColumnIndex),
            rawId = FirstClassByteArray(it.getBlob(rawTransactionIdIndex)),
            minedHeight = BlockHeight.new(zcashNetwork, it.getLong(minedHeightColumnIndex)),
            expiryHeight = BlockHeight.new(zcashNetwork, it.getLong(expiryHeightIndex)),
            index = it.getLong(transactionIndexColumnIndex),
            raw = FirstClassByteArray(it.getBlob(rawIndex)),
            isSentTransaction = isSent,
            netValue = Zatoshi(netValueLong.absoluteValue),
            feePaid = Zatoshi(it.getLong(feePaidIndex)),
            isChange = it.getInt(isChangeIndex) != 0,
            isWalletInternal = it.getInt(isWalletInternalIndex) != 0,
            receivedNoteCount = it.getInt(receivedNoteCountIndex),
            sentNoteCount = it.getInt(sentNoteCountIndex),
            memoCount = it.getInt(memoCountIndex),
            blockTimeEpochSeconds = it.getLong(blockTimeIndex)
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
    const val VIEW_NAME = "v_transactions" // $NON-NLS

    const val COLUMN_INTEGER_ID = "id_tx" // $NON-NLS

    const val COLUMN_INTEGER_MINED_HEIGHT = "mined_height" // $NON-NLS

    const val COLUMN_INTEGER_TRANSACTION_INDEX = "tx_index" // $NON-NLS

    const val COLUMN_BLOB_RAW_TRANSACTION_ID = "txid" // $NON-NLS

    const val COLUMN_INTEGER_EXPIRY_HEIGHT = "expiry_height" // $NON-NLS

    const val COLUMN_BLOB_RAW = "raw" // $NON-NLS

    const val COLUMN_LONG_VALUE = "net_value" // $NON-NLS

    const val COLUMN_LONG_FEE_PAID = "fee_paid" // $NON-NLS

    const val COLUMN_BOOLEAN_IS_WALLET_INTERNAL = "is_wallet_internal" // $NON-NLS

    const val COLUMN_BOOLEAN_IS_CHANGE = "has_change" // $NON-NLS

    const val COLUMN_INTEGER_SENT_NOTE_COUNT = "sent_note_count" // $NON-NLS

    const val COLUMN_INTEGER_RECEIVED_NOTE_COUNT = "received_note_count" // $NON-NLS

    const val COLUMN_INTEGER_MEMO_COUNT = "memo_count" // $NON-NLS

    const val COLUMN_INTEGER_BLOCK_TIME = "block_time" // $NON-NLS
}
