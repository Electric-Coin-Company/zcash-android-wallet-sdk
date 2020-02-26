package cash.z.wallet.sdk.demoapp.demos.getaddress

import android.view.LayoutInflater
import cash.z.wallet.sdk.Initializer
import cash.z.wallet.sdk.demoapp.App
import cash.z.wallet.sdk.demoapp.BaseDemoFragment
import cash.z.wallet.sdk.demoapp.databinding.FragmentGetAddressBinding

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