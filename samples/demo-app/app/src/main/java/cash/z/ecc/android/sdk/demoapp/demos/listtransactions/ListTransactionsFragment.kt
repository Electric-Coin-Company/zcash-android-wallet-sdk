package cash.z.ecc.android.sdk.demoapp.demos.listtransactions

import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.demoapp.App
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentListTransactionsBinding
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.twig
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * List all transactions related to the given seed, since the given birthday. This begins by
 * downloading any missing blocks and then validating and scanning their contents. Once scan is
 * complete, the transactions are available in the database and can be accessed by any SQL tool.
 * By default, the SDK uses a PagedTransactionRepository to provide transaction contents from the
 * database in a paged format that works natively with RecyclerViews.
 */
class ListTransactionsFragment : BaseDemoFragment<FragmentListTransactionsBinding>() {
    private val config = App.instance.defaultConfig
    private val initializer = Initializer(App.instance, host = config.host, port = config.port)
    private val birthday = config.loadBirthday()
    private lateinit var synchronizer: Synchronizer
    private lateinit var adapter: TransactionAdapter<ConfirmedTransaction>
    private lateinit var address: String
    private var status: Synchronizer.Status? = null

    private val isSynced get() = status == Synchronizer.Status.SYNCED

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentListTransactionsBinding =
        FragmentListTransactionsBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        initializer.new(config.seed, birthday)
        synchronizer = Synchronizer(initializer)
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
            address = synchronizer.getAddress()
            synchronizer.receivedTransactions.onEach {
                onTransactionsUpdated(it)
            }.launchIn(this)
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
        synchronizer.processorInfo.collectWith(lifecycleScope, ::onProcessorInfoUpdated)
        synchronizer.progress.collectWith(lifecycleScope, ::onProgress)
    }

    private fun onProcessorInfoUpdated(info: CompactBlockProcessor.ProcessorInfo) {
        if (info.isScanning) binding.textInfo.text = "Scanning blocks...${info.scanProgress}%"
    }

    private fun onProgress(i: Int) {
        if (i < 100) binding.textInfo.text = "Downloading blocks...$i%"
    }

    private fun onStatus(status: Synchronizer.Status) {
        this.status = status
        binding.textStatus.text = "Status: $status"
        if (isSynced) onSyncComplete()
    }

    private fun onSyncComplete() {
        binding.textInfo.visibility = View.INVISIBLE
    }

    private fun onTransactionsUpdated(transactions: PagedList<ConfirmedTransaction>) {
        twig("got a new paged list of transactions")
        adapter.submitList(transactions)

        // show message when there are no transactions
        if (isSynced) {
            binding.textInfo.apply {
                if (transactions.isEmpty()) {
                    visibility = View.VISIBLE
                    text =
                        "No transactions found. Try to either change the seed words in the" +
                                " DemoConfig.kt file or send funds to this address (tap the FAB to copy it):\n\n $address"
                } else {
                    visibility = View.INVISIBLE
                    text = ""
                }
            }
        }
    }

    override fun onActionButtonClicked() {
        if (::address.isInitialized) copyToClipboard(address)
    }
}
