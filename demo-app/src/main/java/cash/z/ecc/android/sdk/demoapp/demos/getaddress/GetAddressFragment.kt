package cash.z.ecc.android.sdk.demoapp.demos.getaddress

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetAddressBinding
import cash.z.ecc.android.sdk.demoapp.ext.requireApplicationContext
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.UnifiedFullViewingKey
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Displays the address associated with the seed defined by the default config. To modify the seed
 * that is used, update the `DemoConfig.seedWords` value.
 */
class GetAddressFragment : BaseDemoFragment<FragmentGetAddressBinding>() {

    private lateinit var viewingKey: UnifiedFullViewingKey
    private lateinit var seed: ByteArray

    /**
     * Initialize the required values that would normally live outside the demo but are repeated
     * here for completeness so that each demo file can serve as a standalone example.
     */
    private fun setup() {
        // defaults to the value of `DemoConfig.seedWords` but can also be set by the user
        val seedPhrase = sharedViewModel.seedPhrase.value

        // Use a BIP-39 library to convert a seed phrase into a byte array. Most wallets already
        // have the seed stored
        seed = Mnemonics.MnemonicCode(seedPhrase).toSeed()

        // the derivation tool can be used for generating keys and addresses
        viewingKey = runBlocking {
            DerivationTool.deriveUnifiedFullViewingKeys(
                seed,
                ZcashNetwork.fromResources(requireApplicationContext())
            ).first()
        }
    }

    private fun displayAddress() {
        // a full fledged app would just get the address from the synchronizer
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            val uaddress = DerivationTool.deriveUnifiedAddress(
                seed,
                ZcashNetwork.fromResources(requireApplicationContext())
            )
            binding.textInfo.text = "address:\n$uaddress"
        }
    }

    // TODO [#677]: Show an example with the synchronizer
    // TODO [#677]: https://github.com/zcash/zcash-android-wallet-sdk/issues/677

    //
    // Android Lifecycle overrides
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup()
    }

    override fun onResume() {
        super.onResume()
        displayAddress()
    }

    //
    // Base Fragment overrides
    //

    override fun onActionButtonClicked() {
        viewLifecycleOwner.lifecycleScope.launch {
            copyToClipboard(
                DerivationTool.deriveUnifiedAddress(
                    viewingKey.encoding,
                    ZcashNetwork.fromResources(requireApplicationContext())
                ),
                "Unified address copied to clipboard!"
            )
        }
    }

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetAddressBinding =
        FragmentGetAddressBinding.inflate(layoutInflater)
}
