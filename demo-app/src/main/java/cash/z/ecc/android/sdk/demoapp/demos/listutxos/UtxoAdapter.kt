package cash.z.ecc.android.sdk.demoapp.demos.listutxos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.model.Transaction

/**
 * Simple adapter implementation that knows how to bind a recyclerview to ClearedTransactions.
 */
class UtxoAdapter : ListAdapter<Transaction, UtxoViewHolder>(
    object : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(
            oldItem: Transaction,
            newItem: Transaction
        ) = oldItem.minedHeight() == newItem.minedHeight()

        override fun areContentsTheSame(
            oldItem: Transaction,
            newItem: Transaction
        ) = oldItem == newItem
    }
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = UtxoViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
    )

    override fun onBindViewHolder(
        holder: UtxoViewHolder,
        position: Int
    ) = holder.bindTo(getItem(position))
}

private fun Transaction.minedHeight() = when (this) {
    is Transaction.Received -> minedHeight
    is Transaction.Sent -> minedHeight
}
