package cash.z.ecc.android.sdk.internal.db.derived

import android.database.sqlite.SQLiteDatabase
import cash.z.ecc.android.sdk.internal.db.queryAndMap
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.EncodedTransaction
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Locale

internal class TransactionTable(private val zcashNetwork: ZcashNetwork, private val sqliteDatabase: SQLiteDatabase) {

    companion object {
        private val SELECTION_BLOCK_IS_NULL = String.format(
            Locale.ROOT,
            "%s IS NULL", // $NON-NLS
            TransactionTableDefinition.COLUMN_INTEGER_BLOCK
        )

        private val SELECTION_BLOCK_HEIGHT = String.format(
            Locale.ROOT,
            "%s = ?", // $NON-NLS
            TransactionTableDefinition.COLUMN_INTEGER_BLOCK
        )

        private val PROJECTION_COUNT = arrayOf("COUNT(*)")

        private val PROJECTION_ENCODED_TRANSACTION = arrayOf(
            TransactionTableDefinition.COLUMN_BLOB_TRANSACTION_ID,
            TransactionTableDefinition.COLUMN_BLOB_RAW,
            TransactionTableDefinition.COLUMN_INTEGER_EXPIRY_HEIGHT
        )

        private val SELECTION_TRANSACTION_ID_AND_RAW_NOT_NULL = String.format(
            Locale.ROOT,
            "%s = ? AND %s IS NOT NULL", // $NON-NLS
            TransactionTableDefinition.COLUMN_INTEGER_ID,
            TransactionTableDefinition.COLUMN_BLOB_RAW
        )
    }

    suspend fun count() = withContext(Dispatchers.IO) {
        sqliteDatabase.queryAndMap(
            table = TransactionTableDefinition.TABLE_NAME,
            columns = PROJECTION_COUNT,
            cursorParser = { it.getLong(0) }
        ).first()
    }

    suspend fun countUnmined() =
        sqliteDatabase.queryAndMap(
            table = TransactionTableDefinition.TABLE_NAME,
            columns = PROJECTION_COUNT,
            selection = SELECTION_BLOCK_IS_NULL,
            cursorParser = { it.getLong(0) }
        ).first()

    suspend fun findEncodedTransactionById(id: Long): EncodedTransaction? {
        return sqliteDatabase.queryAndMap(
            table = TransactionTableDefinition.TABLE_NAME,
            columns = PROJECTION_ENCODED_TRANSACTION,
            selection = SELECTION_TRANSACTION_ID_AND_RAW_NOT_NULL,
            selectionArgs = arrayOf(id.toString())
        ) {
            val txIdIndex = it.getColumnIndexOrThrow(TransactionTableDefinition.COLUMN_BLOB_TRANSACTION_ID)
            val rawIndex = it.getColumnIndexOrThrow(TransactionTableDefinition.COLUMN_BLOB_RAW)
            val heightIndex = it.getColumnIndexOrThrow(TransactionTableDefinition.COLUMN_INTEGER_EXPIRY_HEIGHT)

            val txid = it.getBlob(txIdIndex)
            val raw = it.getBlob(rawIndex)
            val expiryHeight = if (it.isNull(heightIndex)) {
                null
            } else {
                BlockHeight.new(zcashNetwork, it.getLong(heightIndex))
            }

            EncodedTransaction(
                FirstClassByteArray(txid),
                FirstClassByteArray(raw),
                expiryHeight
            )
        }.firstOrNull()
    }
}

object TransactionTableDefinition {
    const val TABLE_NAME = "transactions"

    const val COLUMN_INTEGER_ID = "id_tx"

    const val COLUMN_BLOB_TRANSACTION_ID = "txid"

    const val COLUMN_TEXT_CREATED = "created"

    const val COLUMN_INTEGER_BLOCK = "block"

    const val COLUMN_INTEGER_TX_INDEX = "tx_index"

    const val COLUMN_INTEGER_EXPIRY_HEIGHT = "expiry_height"

    const val COLUMN_BLOB_RAW = "raw"
}
