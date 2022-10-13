package cash.z.ecc.android.sdk.internal.db.derived

import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.ecc.android.sdk.internal.db.queryAndMap
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.Transaction
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.flow.first
import java.util.Locale

internal class SentTransactionView(
    private val zcashNetwork: ZcashNetwork,
    private val sqliteDatabase: SupportSQLiteDatabase
) {
    companion object {

        private val ORDER_BY = String.format(
            Locale.ROOT,
            "%s DESC, %s DESC", // $NON-NLS
            SentTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT,
            SentTransactionViewDefinition.COLUMN_INTEGER_ID
        )

        private val PROJECTION_COUNT = arrayOf("COUNT(*)") // $NON-NLS
    }

    suspend fun count() = sqliteDatabase.queryAndMap(
        SentTransactionViewDefinition.VIEW_NAME,
        columns = PROJECTION_COUNT,
        cursorParser = { it.getLong(0) }
    ).first()

    fun getSentTransactions() =
        sqliteDatabase.queryAndMap(
            table = SentTransactionViewDefinition.VIEW_NAME,
            orderBy = ORDER_BY,
            cursorParser = {
                val idColumnIndex = it.getColumnIndex(SentTransactionViewDefinition.COLUMN_INTEGER_ID)
                val minedHeightColumnIndex =
                    it.getColumnIndex(SentTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT)
                val transactionIndexColumnIndex = it.getColumnIndex(
                    SentTransactionViewDefinition
                        .COLUMN_INTEGER_TRANSACTION_INDEX
                )
                val rawTransactionIdIndex =
                    it.getColumnIndex(SentTransactionViewDefinition.COLUMN_BLOB_RAW_TRANSACTION_ID)
                val expiryHeightIndex = it.getColumnIndex(SentTransactionViewDefinition.COLUMN_INTEGER_EXPIRY_HEIGHT)
                val rawIndex = it.getColumnIndex(SentTransactionViewDefinition.COLUMN_BLOB_RAW)
                val sentTotalIndex = it.getColumnIndex(SentTransactionViewDefinition.COLUMN_INTEGER_SENT_TOTAL)
                val sentNoteCountIndex = it.getColumnIndex(SentTransactionViewDefinition.COLUMN_INTEGER_SENT_NOTE_COUNT)
                val memoCountIndex = it.getColumnIndex(SentTransactionViewDefinition.COLUMN_INTEGER_MEMO_COUNT)
                val blockTimeIndex = it.getColumnIndex(SentTransactionViewDefinition.COLUMN_INTEGER_BLOCK_TIME)

                Transaction.Sent(
                    id = it.getLong(idColumnIndex),
                    rawId = FirstClassByteArray(it.getBlob(rawTransactionIdIndex)),
                    minedHeight = BlockHeight.new(zcashNetwork, it.getLong(minedHeightColumnIndex)),
                    expiryHeight = BlockHeight.new(zcashNetwork, it.getLong(expiryHeightIndex)),
                    index = it.getLong(transactionIndexColumnIndex),
                    raw = FirstClassByteArray(it.getBlob(rawIndex)),
                    sentTotal = Zatoshi(it.getLong(sentTotalIndex)),
                    sentNoteCount = it.getInt(sentNoteCountIndex),
                    memoCount = it.getInt(memoCountIndex),
                    time = it.getLong(blockTimeIndex)
                )
            }
        )
}

internal object SentTransactionViewDefinition {
    const val VIEW_NAME = "v_tx_sent" // $NON-NLS

    const val COLUMN_INTEGER_ID = "id_tx" // $NON-NLS

    const val COLUMN_INTEGER_MINED_HEIGHT = "mined_height" // $NON-NLS

    const val COLUMN_INTEGER_TRANSACTION_INDEX = "tx_index" // $NON-NLS

    const val COLUMN_BLOB_RAW_TRANSACTION_ID = "txid" // $NON-NLS

    const val COLUMN_INTEGER_EXPIRY_HEIGHT = "expiry_height" // $NON-NLS

    const val COLUMN_BLOB_RAW = "raw" // $NON-NLS

    const val COLUMN_INTEGER_SENT_TOTAL = "sent_total" // $NON-NLS

    const val COLUMN_INTEGER_SENT_NOTE_COUNT = "sent_note_count" // $NON-NLS

    const val COLUMN_INTEGER_MEMO_COUNT = "memo_count" // $NON-NLS

    const val COLUMN_INTEGER_BLOCK_TIME = "block_time" // $NON-NLS
}
