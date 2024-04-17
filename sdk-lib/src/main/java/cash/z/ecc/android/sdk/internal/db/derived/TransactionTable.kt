package cash.z.ecc.android.sdk.internal.db.derived

import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.ecc.android.sdk.internal.db.queryAndMap
import cash.z.ecc.android.sdk.internal.model.EncodedTransaction
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.Locale

internal class TransactionTable(
    private val zcashNetwork: ZcashNetwork,
    private val sqliteDatabase: SupportSQLiteDatabase
) {
    companion object {
        private val SELECTION_BLOCK_IS_NULL =
            String.format(
                Locale.ROOT,
                // $NON-NLS
                "%s IS NULL",
                TransactionTableDefinition.COLUMN_INTEGER_BLOCK
            )

        private val PROJECTION_COUNT = arrayOf("COUNT(*)") // $NON-NLS

        private val PROJECTION_BLOCK = arrayOf(TransactionTableDefinition.COLUMN_INTEGER_BLOCK)

        private val PROJECTION_PRIMARY_KEY_ID = arrayOf(TransactionTableDefinition.COLUMN_INTEGER_ID)

        private val PROJECTION_ENCODED_TRANSACTION =
            arrayOf(
                TransactionTableDefinition.COLUMN_BLOB_TRANSACTION_ID,
                TransactionTableDefinition.COLUMN_BLOB_RAW,
                TransactionTableDefinition.COLUMN_INTEGER_EXPIRY_HEIGHT
            )

        private val SELECTION_RAW_TRANSACTION_ID =
            String.format(
                Locale.ROOT,
                // $NON-NLS
                "%s = ?",
                TransactionTableDefinition.COLUMN_BLOB_TRANSACTION_ID
            )

        private val SELECTION_TRANSACTION_ID_AND_RAW_NOT_NULL =
            String.format(
                Locale.ROOT,
                // $NON-NLS
                "%s = ? AND %s IS NOT NULL",
                TransactionTableDefinition.COLUMN_BLOB_TRANSACTION_ID,
                TransactionTableDefinition.COLUMN_BLOB_RAW
            )
    }

    suspend fun count() =
        sqliteDatabase.queryAndMap(
            table = TransactionTableDefinition.TABLE_NAME,
            columns = PROJECTION_COUNT,
            cursorParser = { it.getLong(0) }
        ).first()

    suspend fun countUnmined() =
        sqliteDatabase.queryAndMap(
            table = TransactionTableDefinition.TABLE_NAME,
            columns = PROJECTION_COUNT,
            selection = SELECTION_BLOCK_IS_NULL,
            cursorParser = { it.getLong(0) }
        ).first()

    suspend fun findEncodedTransactionByTxId(txId: FirstClassByteArray): EncodedTransaction? {
        return sqliteDatabase.queryAndMap(
            table = TransactionTableDefinition.TABLE_NAME,
            columns = PROJECTION_ENCODED_TRANSACTION,
            selection = SELECTION_TRANSACTION_ID_AND_RAW_NOT_NULL,
            selectionArgs = arrayOf(txId.byteArray)
        ) {
            val rawIndex = it.getColumnIndexOrThrow(TransactionTableDefinition.COLUMN_BLOB_RAW)
            val heightIndex = it.getColumnIndexOrThrow(TransactionTableDefinition.COLUMN_INTEGER_EXPIRY_HEIGHT)

            val raw = it.getBlob(rawIndex)
            val expiryHeight =
                if (it.isNull(heightIndex)) {
                    null
                } else {
                    BlockHeight.new(zcashNetwork, it.getLong(heightIndex))
                }

            EncodedTransaction(
                txId,
                FirstClassByteArray(raw),
                expiryHeight
            )
        }.firstOrNull()
    }

    suspend fun findMinedHeight(rawTransactionId: ByteArray): BlockHeight? {
        return sqliteDatabase.queryAndMap(
            table = TransactionTableDefinition.TABLE_NAME,
            columns = PROJECTION_BLOCK,
            selection = SELECTION_RAW_TRANSACTION_ID,
            selectionArgs = arrayOf(rawTransactionId)
        ) {
            val blockIndex = it.getColumnIndexOrThrow(TransactionTableDefinition.COLUMN_INTEGER_BLOCK)
            BlockHeight.new(zcashNetwork, it.getLong(blockIndex))
        }.firstOrNull()
    }

    suspend fun findDatabaseId(rawTransactionId: ByteArray): Long? {
        return sqliteDatabase.queryAndMap(
            table = TransactionTableDefinition.TABLE_NAME,
            columns = PROJECTION_PRIMARY_KEY_ID,
            selection = SELECTION_RAW_TRANSACTION_ID,
            selectionArgs = arrayOf(rawTransactionId)
        ) {
            val idIndex = it.getColumnIndexOrThrow(TransactionTableDefinition.COLUMN_INTEGER_ID)
            it.getLong(idIndex)
        }.firstOrNull()
    }
}

internal object TransactionTableDefinition {
    const val TABLE_NAME = "transactions" // $NON-NLS

    const val COLUMN_INTEGER_ID = "id_tx" // $NON-NLS

    const val COLUMN_BLOB_TRANSACTION_ID = "txid" // $NON-NLS

    const val COLUMN_TEXT_CREATED = "created" // $NON-NLS

    const val COLUMN_INTEGER_BLOCK = "block" // $NON-NLS

    const val COLUMN_INTEGER_TX_INDEX = "tx_index" // $NON-NLS

    const val COLUMN_INTEGER_EXPIRY_HEIGHT = "expiry_height" // $NON-NLS

    const val COLUMN_BLOB_RAW = "raw" // $NON-NLS
}
