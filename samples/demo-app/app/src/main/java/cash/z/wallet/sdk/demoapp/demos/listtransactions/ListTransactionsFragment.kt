package cash.z.wallet.sdk.demoapp.demos.listtransactions

import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import cash.z.wallet.sdk.data.PagedTransactionRepository
import cash.z.wallet.sdk.data.Synchronizer
import cash.z.wallet.sdk.demoapp.BaseDemoFragment
import cash.z.wallet.sdk.demoapp.databinding.FragmentListTransactionsBinding
import cash.z.wallet.sdk.entity.ReceivedTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * List all transactions from a given seed and birthdate, defined in the Injector class which is
 * intended to mimic dependency injection.
 */
class ListTransactionsFragment : BaseDemoFragment<FragmentListTransactionsBinding>() {
    private lateinit var ledger: PagedTransactionRepository
    private lateinit var synchronizer: Synchronizer

    override fun inflateBinding(layoutInflater: LayoutInflater) =
        FragmentListTransactionsBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        ledger = Injector.provideLedger()
        synchronizer = Injector.provideSynchronizer(ledger)
    }

    override fun onResetComplete() {
        initTransactionUI()
        startSynchronizer()
        monitorStatus()
    }

    override fun onClear() {
        ledger.close()
        synchronizer.stop()
    }

    private fun monitorStatus() {
        lifecycleScope.launch {
            synchronizer.status.collect { onStatus(it) }
        }
    }

    private fun initTransactionUI() {
        binding.recyclerTransactions.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        binding.recyclerTransactions.adapter = TransactionAdapter()
    }

    private fun startSynchronizer() {
        lifecycleScope.apply {
            synchronizer.start(this)
            launchProgressMonitor(synchronizer.progress())
        }
    }

    private fun CoroutineScope.launchProgressMonitor(channel: ReceiveChannel<Int>) = launch {
        for (i in channel) {
            onProgress(i)
        }
    }

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
        ledger.setTransactionPageListener(this) { t ->
            val adapter = binding.recyclerTransactions.adapter as TransactionAdapter
            adapter.submitList(t as PagedList<ReceivedTransaction>)
        }
    }
}