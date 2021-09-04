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
class UtxoAdapter<T : ConfirmedTransaction> :
    ListAdapter<T, UtxoViewHolder<T>>(
        object : DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame(
                oldItem: T,
                newItem: T
            ) = oldItem.minedHeight == newItem.minedHeight

            override fun areContentsTheSame(
                oldItem: T,
                newItem: T
            ) = oldItem == newItem
        }
    ) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = UtxoViewHolder<T>(
        LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
    )

    override fun onBindViewHolder(
        holder: UtxoViewHolder<T>,
        position: Int
    ) = holder.bindTo(getItem(position))

}
