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

/**
 * Displays the available balance && total balance associated with the seed defined by the default config.
 * comments.
 */
class GetBalanceFragment : BaseDemoFragment<FragmentGetBalanceBinding>() {

    private lateinit var synchronizer: Synchronizer

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
                synchronizer = Synchronizer(initializer)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // the lifecycleScope is used to dispose of the synchronize when the fragment dies
        synchronizer.start(lifecycleScope)
        monitorChanges()
    }

    private fun monitorChanges() {
        synchronizer.status.collectWith(lifecycleScope, ::onStatus)
        synchronizer.balances.collectWith(lifecycleScope, ::onBalance)
    }

    private fun onBalance(balance: CompactBlockProcessor.WalletBalance) {
            binding.textBalance.text = """
                Available balance: ${balance.availableZatoshi.convertZatoshiToZecString(12)}
                Total balance: ${balance.totalZatoshi.convertZatoshiToZecString(12)}
            """.trimIndent()

    }


    private fun onStatus(status: Synchronizer.Status) {
        binding.textBalance.text = "Status: $status"
        if (CompactBlockProcessor.WalletBalance().none()) {
            binding.textBalance.text = "Calculating balance..."
        } else {
            onBalance(synchronizer.latestBalance)
        }
    }

    /**
     * Extension function which checks if the balance has been updated or its -1
     */
    private fun CompactBlockProcessor.WalletBalance.none(): Boolean{
        if(synchronizer.latestBalance.totalZatoshi == -1L
            && synchronizer.latestBalance.availableZatoshi == -1L) return true
        return false
    }
}
