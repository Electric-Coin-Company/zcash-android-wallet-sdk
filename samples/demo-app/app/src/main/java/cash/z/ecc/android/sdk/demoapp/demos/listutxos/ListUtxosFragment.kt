package cash.z.ecc.android.sdk.demoapp.demos.listutxos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.demoapp.App
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentListUtxosBinding
import cash.z.ecc.android.sdk.demoapp.util.mainActivity
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.service.LightWalletService
import cash.z.ecc.android.sdk.rpc.LocalRpcTypes
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
class ListUtxosFragment : BaseDemoFragment<FragmentListUtxosBinding>() {
    private val config = App.instance.defaultConfig
    private lateinit var seed: ByteArray
    private lateinit var initializer: SdkSynchronizer.SdkInitializer
    private lateinit var synchronizer: Synchronizer
    private lateinit var adapter: UtxoAdapter<ConfirmedTransaction>
    private val address: String = "t1RwbKka1CnktvAJ1cSqdn7c6PXWG4tZqgd"
    private var status: Synchronizer.Status? = null

    private val isSynced get() = status == Synchronizer.Status.SYNCED

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentListUtxosBinding =
        FragmentListUtxosBinding.inflate(layoutInflater)

    /**
     * Initialize the required values that would normally live outside the demo but are repeated
     * here for completeness so that each demo file can serve as a standalone example.
     */
    private fun setup() {
        // Use a BIP-39 library to convert a seed phrase into a byte array. Most wallets already
        // have the seed stored
        seed = Mnemonics.MnemonicCode(sharedViewModel.seedPhrase.value).toSeed()
        initializer = Initializer(App.instance) {
            it.importWallet(seed, config.birthdayHeight)
            it.alias = "Demo_Utxos"
        }
        synchronizer = Synchronizer(initializer)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setup()
    }

    fun initUi() {
        binding.inputAddress.setText(address)
        binding.inputRangeStart.setText(ZcashSdk.SAPLING_ACTIVATION_HEIGHT.toString())
        binding.inputRangeEnd.setText(config.utxoEndHeight.toString())

        binding.buttonLoad.setOnClickListener {
            mainActivity()?.hideKeyboard()
            downloadTransactions()
        }

        initTransactionUi()
    }

    fun downloadTransactions() {

        binding.textStatus.text = "loading..."
        binding.textStatus.post {
            binding.textStatus.requestFocus()
            val addressToUse = binding.inputAddress.text.toString()
            val startToUse = binding.inputRangeStart.text.toString().toIntOrNull() ?: ZcashSdk.SAPLING_ACTIVATION_HEIGHT
            val endToUse = binding.inputRangeEnd.text.toString().toIntOrNull() ?: config.utxoEndHeight
            var allStart = now
            twig("loading transactions in range $startToUse..$endToUse")
            val txids = lightwalletService?.getTAddressTransactions(addressToUse, startToUse..endToUse)
            var delta = now - allStart
            updateStatus("found ${txids?.size} transactions in ${delta}ms.", false)

            txids?.map {
                it.data.apply {
                    try {
                        initializer.rustBackend.decryptAndStoreTransaction(toByteArray())
                    } catch (t: Throwable) {
                        twig("failed to decrypt and store transaction due to: $t")
                    }
                }
            }?.let { txData ->
                val parseStart = now
                val tList = LocalRpcTypes.TransactionDataList.newBuilder().addAllData(txData).build()
                val parsedTransactions = initializer.rustBackend.parseTransactionDataList(tList)
                delta = now - parseStart
                updateStatus("parsed txs in ${delta}ms.")
            }
            (synchronizer as SdkSynchronizer).refreshTransactions()
//            val finalCount = (synchronizer as SdkSynchronizer).getTransactionCount()
//            "found ${finalCount - initialCount} shielded outputs.
            delta = now - allStart
            updateStatus("Total time ${delta}ms.")

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    finalCount = (synchronizer as SdkSynchronizer).getTransactionCount()
                    withContext(Dispatchers.Main) {
                        delay(100)
                        updateStatus("Also found ${finalCount - initialCount} shielded txs")
                    }
                }
            }
        }
    }

    private val now get() = System.currentTimeMillis()

    private fun updateStatus(message: String, append: Boolean = true) {
        if (append) {
            binding.textStatus.text = "${binding.textStatus.text} $message"
        } else {
            binding.textStatus.text = message
        }
        twig(message)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    override fun onResume() {
        super.onResume()
        resetInBackground()
        val seed = Mnemonics.MnemonicCode(sharedViewModel.seedPhrase.value).toSeed()
        binding.inputAddress.setText(DerivationTool.deriveTransparentAddress(seed))
    }


    var initialCount: Int = 0
    var finalCount: Int = 0
    fun resetInBackground() {
        try {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    initialCount = (synchronizer as SdkSynchronizer).getTransactionCount()
                }
            }
            synchronizer.clearedTransactions.collectWith(lifecycleScope, ::onTransactionsUpdated)
//            synchronizer.receivedTransactions.collectWith(lifecycleScope, ::onTransactionsUpdated)
        } catch (t: Throwable) {
            twig("failed to start the synchronizer!!! due to : $t")
        }
    }

    fun onResetComplete() {
        initTransactionUi()
        startSynchronizer()
        monitorStatus()
    }

    fun onClear() {
        synchronizer.stop()
    }

    private fun initTransactionUi() {
        binding.recyclerTransactions.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        adapter = UtxoAdapter()
        binding.recyclerTransactions.adapter = adapter
//        lifecycleScope.launch {
////            address = synchronizer.getAddress()
//            synchronizer.receivedTransactions.onEach {
//                onTransactionsUpdated(it)
//            }.launchIn(this)
//        }

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
        if (info.isScanning) binding.textStatus.text = "Scanning blocks...${info.scanProgress}%"
    }

    private fun onProgress(i: Int) {
        if (i < 100) binding.textStatus.text = "Downloading blocks...$i%"
    }

    private fun onStatus(status: Synchronizer.Status) {
        this.status = status
        binding.textStatus.text = "Status: $status"
        if (isSynced) onSyncComplete()
    }

    private fun onSyncComplete() {
        binding.textStatus.visibility = View.INVISIBLE
    }

    private fun onTransactionsUpdated(transactions: PagedList<ConfirmedTransaction>) {
        twig("got a new paged list of transactions of size ${transactions.size}")
        adapter.submitList(transactions)
    }

    override fun onActionButtonClicked() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                twig("current count: ${(synchronizer as SdkSynchronizer).getTransactionCount()}")
                twig("refreshing transactions")
                (synchronizer as SdkSynchronizer).refreshTransactions()
                twig("current count: ${(synchronizer as SdkSynchronizer).getTransactionCount()}")
            }
        }
    }
}
