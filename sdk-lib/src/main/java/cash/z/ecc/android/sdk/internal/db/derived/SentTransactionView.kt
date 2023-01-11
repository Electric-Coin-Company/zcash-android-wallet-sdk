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

internal class SentTransactionView(
    private val zcashNetwork: ZcashNetwork,
    private val sqliteDatabase: SupportSQLiteDatabase
) {
    companion object {

        private const val COLUMN_SORT_HEIGHT = "sort_height"

        private val COLUMNS = arrayOf(
            "*", // $NON-NLS
            @Suppress("MaxLineLength")
            "IFNULL(${SentTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT}, ${UInt.MAX_VALUE}) AS $COLUMN_SORT_HEIGHT" // $NON-NLS
        )

        private val ORDER_BY = String.format(
            Locale.ROOT,
            "%s DESC, %s DESC", // $NON-NLS
            COLUMN_SORT_HEIGHT,
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
            columns = COLUMNS,
            orderBy = ORDER_BY,
            cursorParser = { cursor ->
                val idColumnIndex = cursor.getColumnIndex(SentTransactionViewDefinition.COLUMN_INTEGER_ID)
                val minedHeightColumnIndex =
                    cursor.getColumnIndex(SentTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT)
                val transactionIndexColumnIndex = cursor.getColumnIndex(
                    SentTransactionViewDefinition
                        .COLUMN_INTEGER_TRANSACTION_INDEX
                )
                val rawTransactionIdIndex =
                    cursor.getColumnIndex(SentTransactionViewDefinition.COLUMN_BLOB_RAW_TRANSACTION_ID)
                val expiryHeightIndex = cursor.getColumnIndex(
                    SentTransactionViewDefinition.COLUMN_INTEGER_EXPIRY_HEIGHT
                )
                val rawIndex = cursor.getColumnIndex(SentTransactionViewDefinition.COLUMN_BLOB_RAW)
                val sentFromAccount = cursor.getColumnIndex(
                    SentTransactionViewDefinition.COLUMN_INTEGER_SENT_FROM_ACCOUNT
                )
                val sentTotalIndex = cursor.getColumnIndex(SentTransactionViewDefinition.COLUMN_INTEGER_SENT_TOTAL)
                val sentNoteCountIndex = cursor.getColumnIndex(
                    SentTransactionViewDefinition.COLUMN_INTEGER_SENT_NOTE_COUNT
                )
                val memoCountIndex = cursor.getColumnIndex(SentTransactionViewDefinition.COLUMN_INTEGER_MEMO_COUNT)
                val blockTimeIndex = cursor.getColumnIndex(SentTransactionViewDefinition.COLUMN_INTEGER_BLOCK_TIME)

                val expiryHeightLong = cursor.getLong(expiryHeightIndex)

                Transaction.Sent(
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
                    sentFromAccount = Account(cursor.getInt(sentFromAccount)),
                    sentTotal = Zatoshi(cursor.getLong(sentTotalIndex)),
                    sentNoteCount = cursor.getInt(sentNoteCountIndex),
                    memoCount = cursor.getInt(memoCountIndex),
                    blockTimeEpochSeconds = cursor.getLong(blockTimeIndex)
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

    const val COLUMN_INTEGER_SENT_FROM_ACCOUNT = "sent_from_account" // $NON-NLS

    const val COLUMN_INTEGER_SENT_TOTAL = "sent_total" // $NON-NLS

    const val COLUMN_INTEGER_SENT_NOTE_COUNT = "sent_note_count" // $NON-NLS

    const val COLUMN_INTEGER_MEMO_COUNT = "memo_count" // $NON-NLS

    const val COLUMN_INTEGER_BLOCK_TIME = "block_time" // $NON-NLS
}
