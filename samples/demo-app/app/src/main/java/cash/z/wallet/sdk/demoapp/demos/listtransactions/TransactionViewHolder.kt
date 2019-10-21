package cash.z.wallet.sdk.demoapp.demos.listtransactions

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cash.z.wallet.sdk.demoapp.R
import cash.z.wallet.sdk.entity.ReceivedTransaction
import cash.z.wallet.sdk.ext.convertZatoshiToZecString
import java.text.SimpleDateFormat
import java.util.*

class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val amountText = itemView.findViewById<TextView>(R.id.text_transaction_amount)
    private val timeText = itemView.findViewById<TextView>(R.id.text_transaction_timestamp)
    private val formatter = SimpleDateFormat("M/d h:mma", Locale.getDefault())

    fun bindTo(transaction: ReceivedTransaction?) {
        amountText.text = transaction?.value.convertZatoshiToZecString()
        timeText.text =
            if (transaction == null || transaction?.blockTimeInSeconds == 0L) "Pending"
            else formatter.format(transaction.blockTimeInSeconds * 1000L)
    }
}