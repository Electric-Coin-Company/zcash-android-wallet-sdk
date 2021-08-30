package cash.z.ecc.android.sdk.demoapp.demos.listtransactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction

/**
 * Simple adapter implementation that knows how to bind a recyclerview to ClearedTransactions.
 */
class TransactionAdapter<T : ConfirmedTransaction> :
    PagedListAdapter<T, TransactionViewHolder<T>>(
        object : DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame(
                oldItem: T,
                newItem: T
            ) = oldItem.minedHeight == newItem.minedHeight

            override fun areContentsTheSame(
                oldItem: T,
                newItem: T
            ) = oldItem.equals(newItem)
        }
    ) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = TransactionViewHolder<T>(
        LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
    )

    override fun onBindViewHolder(
        holder: TransactionViewHolder<T>,
        position: Int
    ) = holder.bindTo(getItem(position))

}
