package cash.z.ecc.android.sdk.internal.db.derived

import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.ecc.android.sdk.internal.db.queryAndMap
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.TransactionRecipient
import cash.z.ecc.android.sdk.model.ZcashNetwork
import java.util.Locale

internal class TxOutputsView(
    @Suppress("UnusedPrivateMember")
    private val zcashNetwork: ZcashNetwork,
    private val sqliteDatabase: SupportSQLiteDatabase
) {
    companion object {
        private val ORDER_BY =
            String.format(
                Locale.ROOT,
                "%s ASC", // $NON-NLS
                TxOutputsViewDefinition.COLUMN_BLOB_TRANSACTION_ID
            )

        private val PROJECTION_OUTPUT_INDEX = arrayOf(TxOutputsViewDefinition.COLUMN_INTEGER_OUTPUT_INDEX)

        private val PROJECTION_RECIPIENT =
            arrayOf(
                TxOutputsViewDefinition.COLUMN_STRING_TO_ADDRESS,
                TxOutputsViewDefinition.COLUMN_INTEGER_TO_ACCOUNT
            )

        private val SELECT_BY_TRANSACTION_ID_AND_NOT_CHANGE =
            String.format(
                Locale.ROOT,
                "%s = ? AND %s == 0", // $NON-NLS
                TxOutputsViewDefinition.COLUMN_BLOB_TRANSACTION_ID,
                TxOutputsViewDefinition.COLUMN_INTEGER_IS_CHANGE
            )
    }

    fun getSaplingOutputIndices(transactionId: FirstClassByteArray) =
        sqliteDatabase.queryAndMap(
            table = TxOutputsViewDefinition.VIEW_NAME,
            columns = PROJECTION_OUTPUT_INDEX,
            selection = SELECT_BY_TRANSACTION_ID_AND_NOT_CHANGE,
            selectionArgs = arrayOf(transactionId.byteArray),
            orderBy = ORDER_BY,
            cursorParser = {
                val idColumnOutputIndex = it.getColumnIndex(TxOutputsViewDefinition.COLUMN_INTEGER_OUTPUT_INDEX)

                it.getInt(idColumnOutputIndex)
            }
        )

    fun getRecipients(transactionId: FirstClassByteArray) =
        sqliteDatabase.queryAndMap(
            table = TxOutputsViewDefinition.VIEW_NAME,
            columns = PROJECTION_RECIPIENT,
            selection = SELECT_BY_TRANSACTION_ID_AND_NOT_CHANGE,
            selectionArgs = arrayOf(transactionId.byteArray),
            orderBy = ORDER_BY,
            cursorParser = {
                val toAccountIndex = it.getColumnIndex(TxOutputsViewDefinition.COLUMN_INTEGER_TO_ACCOUNT)
                val toAddressIndex = it.getColumnIndex(TxOutputsViewDefinition.COLUMN_STRING_TO_ADDRESS)

                if (!it.isNull(toAccountIndex)) {
                    TransactionRecipient.Account(Account(it.getInt(toAccountIndex)))
                } else {
                    TransactionRecipient.Address(it.getString(toAddressIndex))
                }
            }
        )
}

internal object TxOutputsViewDefinition {
    const val VIEW_NAME = "v_tx_outputs" // $NON-NLS

    const val COLUMN_BLOB_TRANSACTION_ID = "txid" // $NON-NLS

    const val COLUMN_INTEGER_OUTPUT_POOL = "output_pool" // $NON-NLS

    const val COLUMN_INTEGER_OUTPUT_INDEX = "output_index" // $NON-NLS

    const val COLUMN_INTEGER_FROM_ACCOUNT = "from_account" // $NON-NLS

    const val COLUMN_STRING_TO_ADDRESS = "to_address" // $NON-NLS

    const val COLUMN_INTEGER_TO_ACCOUNT = "to_account" // $NON-NLS

    const val COLUMN_INTEGER_VALUE = "value" // $NON-NLS

    const val COLUMN_INTEGER_IS_CHANGE = "is_change" // $NON-NLS

    const val COLUMN_BLOB_MEMO = "memo" // $NON-NLS
}
