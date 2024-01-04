package cash.z.ecc.android.sdk.demoapp.demos.listtransactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.model.TransactionOverview

/**
 * Simple adapter implementation that knows how to bind a recyclerview to ClearedTransactions.
 */
class TransactionAdapter : ListAdapter<TransactionOverview, TransactionViewHolder>(
    object : DiffUtil.ItemCallback<TransactionOverview>() {
        override fun areItemsTheSame(
            oldItem: TransactionOverview,
            newItem: TransactionOverview
        ) = oldItem.minedHeight == newItem.minedHeight

        override fun areContentsTheSame(
            oldItem: TransactionOverview,
            newItem: TransactionOverview
        ) = oldItem == newItem
    }
) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = TransactionViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
    )

    override fun onBindViewHolder(
        holder: TransactionViewHolder,
        position: Int
    ) = holder.bindTo(getItem(position))
}
