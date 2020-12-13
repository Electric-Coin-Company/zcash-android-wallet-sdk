package cash.z.ecc.android.sdk.demoapp.demos.getbalance

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.demoapp.App
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetBalanceBinding
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.tool.DerivationTool

class GetBalanceFragment : BaseDemoFragment<FragmentGetBalanceBinding>() {

    private lateinit var synchronizer: Synchronizer
    private lateinit var viewingKey: String

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetBalanceBinding =
        FragmentGetBalanceBinding.inflate(layoutInflater)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setup()
    }

    private fun setup() {
        //
        var seedPhrase = sharedViewModel.seedPhrase.value

        val seed = Mnemonics.MnemonicCode(seedPhrase).toSeed()

        viewingKey = DerivationTool.deriveViewingKeys(seed).first()

        App.instance.defaultConfig.let { config ->
            Initializer(App.instance) {
                it.importWallet(viewingKey, config.birthdayHeight)
                it.server(config.host, config.port)
            }.let { initializer ->
                synchronizer = Synchronizer(initializer)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // the lifecycleScope is used to dispose of the synchronizer when the fragment dies
        synchronizer.start(lifecycleScope)
        monitorChanges()
    }

    private fun monitorChanges() {
        synchronizer.status.collectWith(lifecycleScope, ::onStatus)
        synchronizer.balances.collectWith(lifecycleScope, ::onBalance)
    }

    private var isSyncing = true
        set(value) {
            field = value
        }

    private fun onBalance(balance: CompactBlockProcessor.WalletBalance) {
        this.balance = balance
        if (!isSyncing) {
            binding.textBalance.text = """
                Available balance: ${balance.availableZatoshi.convertZatoshiToZecString(12)}
                Total balance: ${balance.totalZatoshi.convertZatoshiToZecString(12)}
            """.trimIndent()
        }
    }

    private var balance = CompactBlockProcessor.WalletBalance()
        set(value) {
            field = value
        }


    private fun onStatus(status: Synchronizer.Status) {
        binding.textBalance.text = "Status: $status"
        isSyncing = status != Synchronizer.Status.SYNCED
        if (status == Synchronizer.Status.SCANNING) {
            binding.textBalance.text = "Calculating balance..."
        } else {
            if (!isSyncing) onBalance(balance)
        }
    }
}
