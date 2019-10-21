package cash.z.wallet.sdk.demoapp.demos.send

import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.lifecycle.coroutineScope
import cash.z.wallet.sdk.data.SdkSynchronizer
import cash.z.wallet.sdk.data.Synchronizer
import cash.z.wallet.sdk.demoapp.App
import cash.z.wallet.sdk.demoapp.BaseDemoFragment
import cash.z.wallet.sdk.demoapp.databinding.FragmentSendBinding
import cash.z.wallet.sdk.demoapp.util.SampleStorageBridge
import cash.z.wallet.sdk.ext.convertZatoshiToZecString
import cash.z.wallet.sdk.ext.convertZecToZatoshi
import cash.z.wallet.sdk.ext.toZec
import cash.z.wallet.sdk.secure.Wallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.filterNot
import kotlinx.coroutines.launch

class SendFragment : BaseDemoFragment<FragmentSendBinding>() {
    private val sampleSeed = App.instance.defaultConfig.seed
    private val birthdayHeight: Int = App.instance.defaultConfig.birthdayHeight
    private val host: String = App.instance.defaultConfig.host

    private lateinit var synchronizer: Synchronizer

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentSendBinding =
        FragmentSendBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        val keyManager = SampleStorageBridge().securelyStoreSeed(sampleSeed)
        synchronizer =
            Synchronizer(App.instance, host, keyManager, birthdayHeight)
    }

    override fun onResetComplete() {
        lifecycle.coroutineScope.apply {
            synchronizer.start(this)
            launchProgressMonitor(synchronizer.progress())
            launchBalanceMonitor(synchronizer.balances())
        }

        binding.buttonSend.setOnClickListener(::onSend)
    }

    override fun onClear() {
        // remove the stored databases
        (synchronizer as SdkSynchronizer).clearData()
        synchronizer.stop()
    }

    private fun CoroutineScope.launchProgressMonitor(channel: ReceiveChannel<Int>) = launch {
        for (i in channel) {
            onProgress(i)
        }
    }

    private fun CoroutineScope.launchBalanceMonitor(
        channel: ReceiveChannel<Wallet.WalletBalance>
    ) = launch {
        val positiveBalances = channel.filterNot { it.total < 0 }
        for (i in positiveBalances) {
            onBalance(i)
        }
    }

    private fun onProgress(i: Int) {
        val message = when (i) {
            100 -> "Scanning blocks..."
            else -> "Downloading blocks...$i%"
        }
        binding.textStatus.text = message
    }

    private fun onBalance(balance: Wallet.WalletBalance) {
        binding.textBalances.text = """
            Available balance: ${balance.available.convertZatoshiToZecString()}
            Total balance: ${balance.total.convertZatoshiToZecString()}
        """.trimIndent()
        binding.buttonSend.isEnabled = balance.available > 0
        binding.textStatus.text = "Synced!"
    }

    private fun onSend(unused: View) {
        // TODO: add input fields to the UI. Possibly, including a scanner for the address input
        lifecycleScope.launch {
            synchronizer.sendToAddress(0.001.toZec().convertZecToZatoshi(), "ztestsapling1fg82ar8y8whjfd52l0xcq0w3n7nn7cask2scp9rp27njeurr72ychvud57s9tu90fdqgwdt07lg", "Demo App Funds")
        }
        Toast.makeText(App.instance, "Sending funds...", Toast.LENGTH_SHORT).show()
    }
}
