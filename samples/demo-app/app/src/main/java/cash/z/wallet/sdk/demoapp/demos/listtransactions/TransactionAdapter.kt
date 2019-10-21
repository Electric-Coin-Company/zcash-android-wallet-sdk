package cash.z.wallet.sdk.demoapp.demos.listtransactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import cash.z.wallet.sdk.demoapp.R
import cash.z.wallet.sdk.entity.ReceivedTransaction

class TransactionAdapter :
    PagedListAdapter<ReceivedTransaction, TransactionViewHolder>(
        DIFF_CALLBACK
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

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ReceivedTransaction>() {
            override fun areItemsTheSame(
                oldItem: ReceivedTransaction,
                newItem: ReceivedTransaction
            ) = oldItem.minedHeight == newItem.minedHeight

            override fun areContentsTheSame(
                oldItem: ReceivedTransaction,
                newItem: ReceivedTransaction
            ) = oldItem.equals(newItem)
        }
    }
}
    