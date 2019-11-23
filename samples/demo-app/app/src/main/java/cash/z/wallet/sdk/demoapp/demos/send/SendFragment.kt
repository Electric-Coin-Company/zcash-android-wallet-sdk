package cash.z.wallet.sdk.demoapp.demos.send

import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import cash.z.wallet.sdk.Initializer
import cash.z.wallet.sdk.Synchronizer
import cash.z.wallet.sdk.block.CompactBlockProcessor
import cash.z.wallet.sdk.demoapp.App
import cash.z.wallet.sdk.demoapp.BaseDemoFragment
import cash.z.wallet.sdk.demoapp.databinding.FragmentSendBinding
import cash.z.wallet.sdk.demoapp.util.SampleStorageBridge
import cash.z.wallet.sdk.entity.*
import cash.z.wallet.sdk.ext.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SendFragment : BaseDemoFragment<FragmentSendBinding>() {
    private val config = App.instance.defaultConfig
    private val initializer = Initializer(App.instance)

    private lateinit var synchronizer: Synchronizer
    private lateinit var keyManager: SampleStorageBridge

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentSendBinding =
        FragmentSendBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        val spendingKeys = initializer.new(config.seed)
        keyManager = SampleStorageBridge().securelyStorePrivateKey(spendingKeys[0])
        synchronizer = Synchronizer(App.instance, config.host, initializer.rustBackend)
    }

    override fun onResetComplete() {
        initSendUI()
        startSynchronizer()
        monitorStatus()
    }

    private fun initSendUI() {
        binding.buttonSend.setOnClickListener(::onSend)
    }

    private fun startSynchronizer() {
        lifecycleScope.apply {
            synchronizer.start(this)
        }
    }

    private fun monitorStatus() {
        synchronizer.status.collectWith(lifecycleScope, ::onStatus)
        synchronizer.progress.collectWith(lifecycleScope, ::onProgress)
        synchronizer.balances.collectWith(lifecycleScope, ::onBalance)
    }

    private fun onStatus(status: Synchronizer.Status) {
        binding.textStatus.text = "Status: $status"
    }

    override fun onClear() {
        synchronizer.stop()
        initializer.clear()
    }

    private fun onProgress(i: Int) {
        val message = when (i) {
            100 -> "Scanning blocks..."
            else -> "Downloading blocks...$i%"
        }
        binding.textStatus.text = message
    }

    private fun onBalance(balance: CompactBlockProcessor.WalletBalance) {
        binding.textBalances.text = """
            Available balance: ${balance.available.convertZatoshiToZecString()}
            Total balance: ${balance.total.convertZatoshiToZecString()}
        """.trimIndent()
        binding.buttonSend.isEnabled = balance.available > 0
        binding.textStatus.text = "Synced!"
    }

    private fun onSend(unused: View) {
        // TODO: add input fields to the UI. Possibly, including a scanner for the address input
        synchronizer.sendToAddress(
            keyManager.key,
            0.0024.toZec().convertZecToZatoshi(),
            config.toAddress,
            "Demo App Funds"
        ).collectWith(lifecycleScope, ::onPendingTxUpdated)
    }

    private fun onPendingTxUpdated(pendingTransaction: PendingTransaction?) {
        val message = when {
            pendingTransaction == null -> "Transaction not found"
            pendingTransaction.isMined() -> "Transaction Mined!"
            pendingTransaction.isSubmitted() -> "Successfully submitted transaction!"
            pendingTransaction.isFailedEncoding() -> "ERROR: failed to encode transaction!"
            pendingTransaction.isFailedSubmit() -> "ERROR: failed to submit transaction!"
            pendingTransaction.isCreated() -> "Transaction creation complete!"
            pendingTransaction.isCreating() -> "Creating transaction!"
            else -> "Transaction updated!".also { twig("Unhandled TX state: $pendingTransaction") }
        }
        twig("PENDING TX: $message")
        Toast.makeText(App.instance, message, Toast.LENGTH_SHORT).show()
    }
}
