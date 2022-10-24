package cash.z.ecc.android.sdk.internal.db.derived

import androidx.sqlite.db.SupportSQLiteDatabase
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

        private val ORDER_BY = String.format(
            Locale.ROOT,
            "%s DESC, %s DESC", // $NON-NLS
            ReceivedTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT,
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
            orderBy = ORDER_BY,
            cursorParser = {
                val idColumnIndex = it.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_INTEGER_ID)
                val minedHeightColumnIndex =
                    it.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_INTEGER_MINED_HEIGHT)
                val transactionIndexColumnIndex = it.getColumnIndex(
                    ReceivedTransactionViewDefinition
                        .COLUMN_INTEGER_TRANSACTION_INDEX
                )
                val rawTransactionIdIndex =
                    it.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_BLOB_RAW_TRANSACTION_ID)
                val expiryHeightIndex = it.getColumnIndex(
                    ReceivedTransactionViewDefinition.COLUMN_INTEGER_EXPIRY_HEIGHT
                )
                val rawIndex = it.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_BLOB_RAW)
                val receivedAccountIndex = it.getColumnIndex(
                    ReceivedTransactionViewDefinition.COLUMN_INTEGER_RECEIVED_BY_ACCOUNT
                )
                val receivedTotalIndex =
                    it.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_INTEGER_RECEIVED_TOTAL)
                val receivedNoteCountIndex =
                    it.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_INTEGER_RECEIVED_NOTE_COUNT)
                val memoCountIndex = it.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_INTEGER_MEMO_COUNT)
                val blockTimeIndex = it.getColumnIndex(ReceivedTransactionViewDefinition.COLUMN_INTEGER_BLOCK_TIME)

                Transaction.Received(
                    id = it.getLong(idColumnIndex),
                    rawId = FirstClassByteArray(it.getBlob(rawTransactionIdIndex)),
                    minedHeight = BlockHeight.new(zcashNetwork, it.getLong(minedHeightColumnIndex)),
                    expiryHeight = BlockHeight.new(zcashNetwork, it.getLong(expiryHeightIndex)),
                    index = it.getLong(transactionIndexColumnIndex),
                    raw = FirstClassByteArray(it.getBlob(rawIndex)),
                    receivedByAccount = Account(it.getInt(receivedAccountIndex)),
                    receivedTotal = Zatoshi(it.getLong(receivedTotalIndex)),
                    receivedNoteCount = it.getInt(receivedNoteCountIndex),
                    memoCount = it.getInt(memoCountIndex),
                    blockTimeEpochSeconds = it.getLong(blockTimeIndex)
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
