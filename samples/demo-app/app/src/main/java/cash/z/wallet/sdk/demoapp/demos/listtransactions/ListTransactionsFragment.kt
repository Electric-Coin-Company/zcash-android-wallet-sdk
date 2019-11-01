package cash.z.wallet.sdk.demoapp.demos.listtransactions

import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import cash.z.wallet.sdk.Initializer
import cash.z.wallet.sdk.Synchronizer
import cash.z.wallet.sdk.demoapp.App
import cash.z.wallet.sdk.demoapp.BaseDemoFragment
import cash.z.wallet.sdk.demoapp.databinding.FragmentListTransactionsBinding
import cash.z.wallet.sdk.entity.ConfirmedTransaction
import cash.z.wallet.sdk.ext.collectWith
import cash.z.wallet.sdk.ext.twig
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * List all transactions from a given seed and birthdate, defined in the Injector class which is
 * intended to mimic dependency injection.
 */
class ListTransactionsFragment : BaseDemoFragment<FragmentListTransactionsBinding>() {
    private val config = App.instance.defaultConfig
    private val initializer = Initializer(App.instance)
    private lateinit var synchronizer: Synchronizer
    private lateinit var adapter: TransactionAdapter<ConfirmedTransaction>

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentListTransactionsBinding =
        FragmentListTransactionsBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        initializer.new(config.seed)
        synchronizer = Synchronizer(App.instance, config.host, initializer.rustBackend)
    }

    override fun onResetComplete() {
        initTransactionUI()
        startSynchronizer()
        monitorStatus()
    }
    
    override fun onClear() {
        synchronizer.stop()
        initializer.clear()
    }

    private fun initTransactionUI() {
        binding.recyclerTransactions.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        adapter = TransactionAdapter()
        lifecycleScope.launch {
            synchronizer.receivedTransactions.collect { onTransactionsUpdated(it) }
        }
        binding.recyclerTransactions.adapter = adapter
    }

    private fun startSynchronizer() {
        lifecycleScope.apply {
            synchronizer.start(this)
        }
    }

    private fun monitorStatus() {
        synchronizer.status.collectWith(lifecycleScope, ::onStatus)
        synchronizer.progress.collectWith(lifecycleScope, ::onProgress)
    }

//    private fun CoroutineScope.launchProgressMonitor(channel: ReceiveChannel<Int>) = launch {
//        for (i in channel) {
//            onProgress(i)
//        }
//    }

    private fun onProgress(i: Int) {
        val message = when (i) {
            100 -> "Scanning blocks..."
            else -> "Downloading blocks...$i%"
        }
        binding.textInfo.text = message
    }

    private fun onStatus(status: Synchronizer.Status) {
        binding.textStatus.text = "Status: $status"
        if (status == Synchronizer.Status.SYNCED) onSyncComplete()
    }

    private fun onSyncComplete() {
        binding.textInfo.visibility = View.INVISIBLE
    }

    private fun onTransactionsUpdated(transactions: PagedList<ConfirmedTransaction>) {
        twig("got a new paged list of transactions")
        adapter.submitList(transactions)
    }
}
