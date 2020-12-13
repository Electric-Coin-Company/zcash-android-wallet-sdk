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

    private lateinit var synchronize: Synchronizer

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetBalanceBinding =
        FragmentGetBalanceBinding.inflate(layoutInflater)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setup()
    }

    private fun setup() {
        // defaults to the value of `DemoConfig.seedWords` but can also be set by the user
        val seedPhrase = sharedViewModel.seedPhrase.value

        // Use a BIP-39 library to convert a seed phrase into a byte array. Most wallets already
        // have the seed stored
        val seed = Mnemonics.MnemonicCode(seedPhrase).toSeed()

        // converting seed into viewingKey
        val viewingKey = DerivationTool.deriveViewingKeys(seed).first()

        // using the ViewingKey to initialize
        App.instance.defaultConfig.let { config ->
            Initializer(App.instance) {
                it.importWallet(viewingKey, config.birthdayHeight)
                it.server(config.host, config.port)
            }.let { initializer ->
                synchronize = Synchronizer(initializer)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // the lifecycleScope is used to dispose of the synchronize when the fragment dies
        synchronize.start(lifecycleScope)
        monitorChanges()
    }

    private fun monitorChanges() {
        synchronize.status.collectWith(lifecycleScope, ::onStatus)
        synchronize.balances.collectWith(lifecycleScope, ::onBalance)
    }

    private var isSyncing = true

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
