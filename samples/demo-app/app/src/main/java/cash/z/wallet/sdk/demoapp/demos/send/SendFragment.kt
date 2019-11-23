package cash.z.wallet.sdk.demoapp.demos.send

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import cash.z.wallet.sdk.Initializer
import cash.z.wallet.sdk.Synchronizer
import cash.z.wallet.sdk.block.CompactBlockProcessor
import cash.z.wallet.sdk.demoapp.App
import cash.z.wallet.sdk.demoapp.BaseDemoFragment
import cash.z.wallet.sdk.demoapp.R
import cash.z.wallet.sdk.demoapp.databinding.FragmentSendBinding
import cash.z.wallet.sdk.demoapp.util.SampleStorageBridge
import cash.z.wallet.sdk.entity.*
import cash.z.wallet.sdk.ext.*

class SendFragment : BaseDemoFragment<FragmentSendBinding>() {
    private val config = App.instance.defaultConfig
    private val initializer = Initializer(App.instance)

    private lateinit var synchronizer: Synchronizer
    private lateinit var keyManager: SampleStorageBridge

    private lateinit var amountInput: TextView
    private lateinit var addressInput: TextView


    //
    // Observable properties (done without livedata or flows for simplicity)
    //

    private var availableBalance = -1L
        set(value) {
            field = value
            onUpdateSendButton()
        }
    private var isSending = false
        set(value) {
            field = value
            if (value) Twig.sprout("Sending") else Twig.clip("Sending")
            onUpdateSendButton()
        }
    private var isSyncing = true
        set(value) {
            field = value
            onUpdateSendButton()
        }


    //
    // BaseDemoFragment overrides
    //

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentSendBinding =
        FragmentSendBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        val spendingKeys = initializer.new(config.seed)
        keyManager = SampleStorageBridge().securelyStorePrivateKey(spendingKeys[0])
        synchronizer = Synchronizer(App.instance, config.host, initializer.rustBackend)
    }

    // STARTING POINT
    override fun onResetComplete() {
        initSendUi()
        startSynchronizer()
        monitorChanges()
    }

    override fun onClear() {
        synchronizer.stop()
        initializer.clear()
    }


    //
    // Private functions
    //

    private fun initSendUi() {
        amountInput = binding.root.findViewById<TextView>(R.id.input_amount).apply {
            text = config.sendAmount.toString()
        }
        addressInput = binding.root.findViewById<TextView>(R.id.input_address).apply {
            text = config.toAddress
        }
        binding.buttonSend.setOnClickListener(::onSend)
    }

    private fun startSynchronizer() {
        lifecycleScope.apply {
            synchronizer.start(this)
        }
    }

    private fun monitorChanges() {
        synchronizer.status.collectWith(lifecycleScope, ::onStatus)
        synchronizer.progress.collectWith(lifecycleScope, ::onProgress)
        synchronizer.balances.collectWith(lifecycleScope, ::onBalance)
    }

    private fun onStatus(status: Synchronizer.Status) {
        binding.textStatus.text = "Status: $status"
        if (status == Synchronizer.Status.SYNCING) {
            isSyncing = true
            binding.textBalance.text = "Calculating balance..."
        } else {
            isSyncing = false
        }
    }

    private fun onProgress(i: Int) {
        val message = when (i) {
            100 -> "Scanning blocks..."
            else -> "Downloading blocks...$i%"
        }
        binding.textStatus.text = message
        binding.textBalance.text = ""
    }

    private fun onBalance(balance: CompactBlockProcessor.WalletBalance) {
        availableBalance = balance.available
        binding.textBalance.text = """
            Available balance: ${balance.available.convertZatoshiToZecString()}
            Total balance: ${balance.total.convertZatoshiToZecString()}
        """.trimIndent()
    }

    private fun onSend(unused: View) {
        isSending = true
        val amount = amountInput.text.toString().toDouble().convertZecToZatoshi()
        val toAddress = addressInput.text.toString()
        synchronizer.sendToAddress(
            keyManager.key,
            amount,
            toAddress,
            "Demo App Funds"
        ).collectWith(lifecycleScope, ::onPendingTxUpdated)
    }

    private fun onPendingTxUpdated(pendingTransaction: PendingTransaction?) {
        val id = pendingTransaction?.id ?: -1
        val message = when {
            pendingTransaction == null -> "Transaction not found"
            pendingTransaction.isMined() -> "Transaction Mined (id: $id)!\n\nSEND COMPLETE".also { isSending = false }
            pendingTransaction.isSubmitSuccess() -> "Successfully submitted transaction!\nAwaiting confirmation..."
            pendingTransaction.isFailedEncoding() -> "ERROR: failed to encode transaction! (id: $id)".also { isSending = false }
            pendingTransaction.isFailedSubmit() -> "ERROR: failed to submit transaction! (id: $id)".also { isSending = false }
            pendingTransaction.isCreated() -> "Transaction creation complete! (id: $id)"
            pendingTransaction.isCreating() -> "Creating transaction!".also { onResetInfo() }
            else -> "Transaction updated!".also { twig("Unhandled TX state: $pendingTransaction") }
        }
        twig("Pending TX Updated: $message")
        binding.textInfo.apply {
            text = "$text\n$message"
        }
    }

    private fun onUpdateSendButton() {
        with(binding.buttonSend) {
            when {
                isSending -> {
                    text = "➡ sending"
                    isEnabled = false
                }
                isSyncing -> {
                    text = "⌛ syncing"
                    isEnabled = false
                }
                availableBalance <= 0 -> isEnabled = false
                else -> {
                    text = "send"
                    isEnabled = true
                }
            }
        }
    }

    private fun onResetInfo() {
        binding.textInfo.text = "Active Transaction:"
    }

}
