package cash.z.ecc.android.sdk.demoapp.demos.getaddress

import android.view.LayoutInflater
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.demoapp.App
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetAddressBinding

/**
 * Displays the address associated with the seed defined by the default config. To modify the seed
 * that is used, update the `DemoConfig.seedWords` value.
 */
class GetAddressFragment : BaseDemoFragment<FragmentGetAddressBinding>() {

    private var seed: ByteArray = App.instance.defaultConfig.seed
    private val initializer: Initializer = Initializer(App.instance)

    private lateinit var address: String

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetAddressBinding
            = FragmentGetAddressBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        address = initializer.deriveAddress(seed)
    }

    override fun onResetComplete() {
        binding.textInfo.text = address
    }

    override fun onActionButtonClicked() {
        copyToClipboard(address)
    }

}
