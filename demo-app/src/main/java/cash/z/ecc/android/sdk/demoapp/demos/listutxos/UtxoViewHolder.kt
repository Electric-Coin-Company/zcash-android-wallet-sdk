package cash.z.ecc.android.sdk.demoapp.demos.listutxos

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.db.entity.valueInZatoshi
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Simple view holder for displaying confirmed transactions in the recyclerview.
 */
class UtxoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val amountText = itemView.findViewById<TextView>(R.id.text_transaction_amount)
    private val infoText = itemView.findViewById<TextView>(R.id.text_transaction_info)
    private val timeText = itemView.findViewById<TextView>(R.id.text_transaction_timestamp)
    private val formatter = SimpleDateFormat("M/d h:mma", Locale.getDefault())

    @Suppress("MagicNumber")
    fun bindTo(transaction: ConfirmedTransaction?) {
        amountText.text = transaction?.valueInZatoshi.convertZatoshiToZecString()
        timeText.text =
            if (transaction == null || transaction.blockTimeInSeconds == 0L) "Pending"
            else formatter.format(transaction.blockTimeInSeconds * 1000L)
        infoText.text = getMemoString(transaction)
    }

    private fun getMemoString(transaction: ConfirmedTransaction?): String {
        return transaction?.memo?.takeUnless { it[0] < 0 }?.let { String(it) } ?: "no memo"
    }
}
