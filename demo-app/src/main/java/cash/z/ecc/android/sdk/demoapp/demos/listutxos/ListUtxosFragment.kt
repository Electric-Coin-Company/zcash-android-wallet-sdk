package cash.z.ecc.android.sdk.demoapp.demos.listutxos

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentListUtxosBinding
import cash.z.ecc.android.sdk.demoapp.ext.requireApplicationContext
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.demoapp.util.mainActivity
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ===============================================================================================
 * NOTE:  this is still a WIP because t-addrs are not officially supported by the SDK yet
 * ===============================================================================================
 *
 *
 * List all transactions related to the given seed, since the given birthday. This begins by
 * downloading any missing blocks and then validating and scanning their contents. Once scan is
 * complete, the transactions are available in the database and can be accessed by any SQL tool.
 * By default, the SDK uses a PagedTransactionRepository to provide transaction contents from the
 * database in a paged format that works natively with RecyclerViews.
 */
@Suppress("TooManyFunctions")
class ListUtxosFragment : BaseDemoFragment<FragmentListUtxosBinding>() {
    private lateinit var adapter: UtxoAdapter
    private val address: String = "t1RwbKka1CnktvAJ1cSqdn7c6PXWG4tZqgd"
    private var status: Synchronizer.Status? = null

    private val isSynced get() = status == Synchronizer.Status.SYNCED

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentListUtxosBinding =
        FragmentListUtxosBinding.inflate(layoutInflater)

    private fun initUi() {
        binding.inputAddress.setText(address)
        binding.inputRangeStart.setText(
            ZcashNetwork.fromResources(requireApplicationContext()).saplingActivationHeight.toString()
        )
        binding.inputRangeEnd.setText(getUxtoEndHeight(requireApplicationContext()).value.toString())

        binding.buttonLoad.setOnClickListener {
            mainActivity()?.hideKeyboard()
            downloadTransactions()
        }

        initTransactionUi()
    }

    private fun downloadTransactions() {
        sharedViewModel.synchronizerFlow.value?.let { synchronizer ->
            binding.textStatus.text = "loading..."
            binding.textStatus.post {
                // TODO [#973]: Eliminate old UI demo-app
                // TODO [#973]: https://github.com/zcash/zcash-android-wallet-sdk/issues/973
                // val network = ZcashNetwork.fromResources(requireApplicationContext())
                // binding.textStatus.requestFocus()
                // val addressToUse = binding.inputAddress.text.toString()
                // val startToUse = max(
                //     binding.inputRangeStart.text.toString().toLongOrNull()
                //         ?: network.saplingActivationHeight.value,
                //     network.saplingActivationHeight.value
                // )
                // val endToUse = binding.inputRangeEnd.text.toString().toLongOrNull()
                //     ?: getUxtoEndHeight(requireApplicationContext()).value
                // var allStart = now
                // Twig.debug { "loading transactions in range $startToUse..$endToUse" }
                // val txids = lightWalletClient?.getTAddressTransactions(
                //     addressToUse,
                //     BlockHeightUnsafe(startToUse)..BlockHeightUnsafe(endToUse)
                // )
                // var delta = now - allStart
                // updateStatus("found ${txids?.toList()?.size} transactions in ${delta}ms.", false)
                //
                // txids?.map {
                //     // Disabled during migration to newer SDK version; this appears to have been
                //     // leveraging non-public  APIs in the SDK so perhaps should be removed
                //     // it.data.apply {
                //     //     try {
                //     //         runBlocking { initializer.rustBackend.decryptAndStoreTransaction(toByteArray()) }
                //     //     } catch (t: Throwable) {
                //     //         twig("failed to decrypt and store transaction due to: $t")
                //     //     }
                //     // }
                // }?.let { _ ->
                //     // Disabled during migration to newer SDK version; this appears to have been
                //     // leveraging non-public  APIs in the SDK so perhaps should be removed
                //     // val parseStart = now
                //     // val tList = LocalRpcTypes.TransactionDataList.newBuilder().addAllData(txData).build()
                //     // val parsedTransactions = initializer.rustBackend.parseTransactionDataList(tList)
                //     // delta = now - parseStart
                //     // updateStatus("parsed txs in ${delta}ms.")
                // }
                (synchronizer as SdkSynchronizer).refreshTransactions()
                // delta = now - allStart
                // updateStatus("Total time ${delta}ms.")

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        finalCount = synchronizer.getTransactionCount()
                        withContext(Dispatchers.Main) {
                            @Suppress("MagicNumber")
                            delay(100)
                            updateStatus("Also found ${finalCount - initialCount} shielded txs")
                        }
                    }
                }
            }
        }
    }

    // private val now get() = System.currentTimeMillis()

    private fun updateStatus(message: String, append: Boolean = true) {
        if (append) {
            binding.textStatus.text = "${binding.textStatus.text} $message"
        } else {
            binding.textStatus.text = message
        }
        Twig.debug { message }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
        monitorStatus()
    }

    var initialCount: Int = 0
    var finalCount: Int = 0

    private fun initTransactionUi() {
        binding.recyclerTransactions.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        adapter = UtxoAdapter()
        binding.recyclerTransactions.adapter = adapter
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun monitorStatus() {
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
                        .flatMapLatest { it.transactions }
                        .collect { onTransactionsUpdated(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .collect {
                            binding.inputAddress.setText(
                                it.getTransparentAddress(Account.DEFAULT)
                            )
                        }
                }
            }
        }
    }

    private fun onProcessorInfoUpdated(info: CompactBlockProcessor.ProcessorInfo) {
        if (info.isSyncing) binding.textStatus.text = "Syncing blocks...${info.syncProgress}%"
    }

    @Suppress("MagicNumber")
    private fun onProgress(i: Int) {
        if (i < 100) binding.textStatus.text = "Syncing blocks...$i%"
    }

    private fun onStatus(status: Synchronizer.Status) {
        this.status = status
        binding.textStatus.text = "Status: $status"
        if (isSynced) onSyncComplete()
    }

    private fun onSyncComplete() {
        binding.textStatus.visibility = View.INVISIBLE
    }

    private fun onTransactionsUpdated(transactions: List<TransactionOverview>) {
        Twig.debug { "got a new paged list of transactions of size ${transactions.size}" }
        adapter.submitList(transactions)
    }

    override fun onActionButtonClicked() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                sharedViewModel.synchronizerFlow.value?.let { synchronizer ->
                    val sdkSynchronizer = synchronizer as SdkSynchronizer
                    sdkSynchronizer.getTransactionCount().also {
                        Twig.debug { "current count: $it" }
                    }

                    Twig.debug { "refreshing transactions" }
                    sdkSynchronizer.refreshTransactions()
                    sdkSynchronizer.getTransactionCount().also {
                        Twig.debug { "current count: $it" }
                    }
                }
            }
        }
    }

    @Suppress("MagicNumber")
    private fun getUxtoEndHeight(context: Context): BlockHeight {
        return BlockHeight.new(ZcashNetwork.fromResources(context), 968085L)
    }
}
