package cash.z.ecc.android.sdk.demoapp.demos.getbalance

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetBalanceBinding
import cash.z.ecc.android.sdk.demoapp.ext.requireApplicationContext
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.model.LightWalletEndpoint
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.model.defaultForNetwork
import kotlinx.coroutines.flow.filterNotNull

/**
 * Displays the available balance && total balance associated with the seed defined by the default config.
 * comments.
 */
class GetBalanceFragment : BaseDemoFragment<FragmentGetBalanceBinding>() {

    private lateinit var synchronizer: Synchronizer

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetBalanceBinding =
        FragmentGetBalanceBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup()
    }

    private fun setup() {
        // defaults to the value of `DemoConfig.seedWords` but can also be set by the user
        val seedPhrase = sharedViewModel.seedPhrase.value

        // Use a BIP-39 library to convert a seed phrase into a byte array. Most wallets already
        // have the seed stored
        val seed = Mnemonics.MnemonicCode(seedPhrase).toSeed()

        val network = ZcashNetwork.fromResources(requireApplicationContext())
        synchronizer = Synchronizer.newBlocking(
            requireApplicationContext(),
            network,
            lightWalletEndpoint = LightWalletEndpoint.defaultForNetwork(network),
            seed = seed,
            birthday = sharedViewModel.birthdayHeight.value
        )
    }

    override fun onResume() {
        super.onResume()
        // the lifecycleScope is used to dispose of the synchronize when the fragment dies
        synchronizer.start(lifecycleScope)
        monitorChanges()
    }

    private fun monitorChanges() {
        synchronizer.status.collectWith(lifecycleScope, ::onStatus)
        synchronizer.progress.collectWith(lifecycleScope, ::onProgress)
        synchronizer.processorInfo.collectWith(lifecycleScope, ::onProcessorInfoUpdated)
        synchronizer.saplingBalances.filterNotNull().collectWith(lifecycleScope, ::onBalance)
    }

    @Suppress("MagicNumber")
    private fun onBalance(balance: WalletBalance) {
        binding.textBalance.text = """
                Available balance: ${balance.available.convertZatoshiToZecString(12)}
                Total balance: ${balance.total.convertZatoshiToZecString(12)}
        """.trimIndent()
    }

    private fun onStatus(status: Synchronizer.Status) {
        binding.textStatus.text = "Status: $status"
        val balance: WalletBalance? = synchronizer.saplingBalances.value
        if (null == balance) {
            binding.textBalance.text = "Calculating balance..."
        } else {
            onBalance(balance)
        }
    }

    @Suppress("MagicNumber")
    private fun onProgress(i: Int) {
        if (i < 100) {
            binding.textStatus.text = "Downloading blocks...$i%"
        }
    }

    private fun onProcessorInfoUpdated(info: CompactBlockProcessor.ProcessorInfo) {
        if (info.isScanning) binding.textStatus.text = "Scanning blocks...${info.scanProgress}%"
    }
}
