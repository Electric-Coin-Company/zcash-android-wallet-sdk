package cash.z.ecc.android.sdk.demoapp.demos.listtransactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentListTransactionsBinding
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.model.TransactionOverview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * List all transactions related to the given seed, since the given birthday. This begins by
 * downloading any missing blocks and then validating and scanning their contents. Once scan is
 * complete, the transactions are available in the database and can be accessed by any SQL tool.
 * By default, the SDK uses a PagedTransactionRepository to provide transaction contents from the
 * database in a paged format that works natively with RecyclerViews.
 */
@Suppress("TooManyFunctions")
class ListTransactionsFragment : BaseDemoFragment<FragmentListTransactionsBinding>() {
    private lateinit var adapter: TransactionAdapter
    private var status: Synchronizer.Status? = null
    private val isSynced get() = status == Synchronizer.Status.SYNCED

    private fun initTransactionUI() {
        binding.recyclerTransactions.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        adapter = TransactionAdapter()
        binding.recyclerTransactions.adapter = adapter
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun monitorChanges() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.status }
                        .collect { onStatus(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.progress }
                        .collect { onProgress(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.processorInfo }
                        .collect { onProcessorInfoUpdated(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.clearedTransactions }
                        .collect { onTransactionsUpdated(it) }
                }
            }
        }
    }

    //
    // Change listeners
    //

    private fun onProcessorInfoUpdated(info: CompactBlockProcessor.ProcessorInfo) {
        if (info.isScanning) binding.textInfo.text = "Scanning blocks...${info.scanProgress}%"
    }

    @Suppress("MagicNumber")
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

    private fun onTransactionsUpdated(transactions: List<TransactionOverview>) {
        Twig.debug { "got a new paged list of transactions" }
        adapter.submitList(transactions)

        // show message when there are no transactions
        if (isSynced) {
            binding.textInfo.apply {
                if (transactions.isEmpty()) {
                    visibility = View.VISIBLE
                    text =
                        "No transactions found. Try to either change the seed words or send funds to this wallet. " +
                        "The wallet addresses can be found on the Get Address screen."
                } else {
                    visibility = View.INVISIBLE
                    text = ""
                }
            }
        }
    }

    //
    // Android Lifecycle overrides
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTransactionUI()
        monitorChanges()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // We rather hide options menu actions while actively using the Synchronizer
        menu.setGroupVisible(R.id.main_menu_group, false)
    }

    //
    // Base Fragment overrides
    //

    override fun onActionButtonClicked() {
        // no action connected
    }

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentListTransactionsBinding =
        FragmentListTransactionsBinding.inflate(layoutInflater)
}
