package cash.z.ecc.android.sdk.demoapp.demos.listutxos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.demoapp.R

/**
 * Simple adapter implementation that knows how to bind a recyclerview to ClearedTransactions.
 */
class UtxoAdapter : ListAdapter<ConfirmedTransaction, UtxoViewHolder>(
        object : DiffUtil.ItemCallback<ConfirmedTransaction>() {
            override fun areItemsTheSame(
                oldItem: ConfirmedTransaction,
                newItem: ConfirmedTransaction
            ) = oldItem.minedHeight == newItem.minedHeight

            override fun areContentsTheSame(
                oldItem: ConfirmedTransaction,
                newItem: ConfirmedTransaction
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
