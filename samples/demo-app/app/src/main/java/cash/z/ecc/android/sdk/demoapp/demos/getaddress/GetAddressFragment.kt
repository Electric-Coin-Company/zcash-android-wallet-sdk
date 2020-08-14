package cash.z.ecc.android.sdk.demoapp.demos.getaddress

import android.os.Bundle
import android.view.LayoutInflater
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.demoapp.App
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetAddressBinding

/**
 * Displays the address associated with the seed defined by the default config. To modify the seed
 * that is used, update the `DemoConfig.seedWords` value.
 */
class GetAddressFragment : BaseDemoFragment<FragmentGetAddressBinding>() {

    private lateinit var initializer: Initializer
    private lateinit var viewingKey: String
    private lateinit var seed: ByteArray

    /**
     * Initialize the required values that would normally live outside the demo but are repeated
     * here for completeness so that each demo file can serve as a standalone example.
     */
    fun setup() {
        // defaults to the value of `DemoConfig.seedWords` but can also be set by the user
        var seedPhrase = sharedViewModel.seedPhrase.value

        // Use a BIP-39 library to convert a seed phrase into a byte array. Most wallets already
        // have the seed stored
        seed = Mnemonics.MnemonicCode(seedPhrase).toSeed()

        // the initializer loads rust libraries and helps with configuration
        initializer = Initializer(App.instance)

        // demonstrate deriving viewing keys for five accounts but only take the first one
        viewingKey = initializer.deriveViewingKeys(seed).first()
    }

    fun displayAddress() {
        // alternatively, `deriveAddress` can take the seed as a parameter instead
        val address = initializer.deriveAddress(viewingKey)
        binding.textInfo.text = address
    }

    // TODO: show an example with the synchronizer

    //
    // Android Lifecycle overrides
    //

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
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
        copyToClipboard(initializer.deriveAddress(viewingKey))
    }

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetAddressBinding =
        FragmentGetAddressBinding.inflate(layoutInflater)

}
