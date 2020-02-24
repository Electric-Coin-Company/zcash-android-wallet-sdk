package cash.z.wallet.sdk.demoapp.demos.getaddress

import android.view.LayoutInflater
import cash.z.wallet.sdk.Initializer
import cash.z.wallet.sdk.demoapp.App
import cash.z.wallet.sdk.demoapp.BaseDemoFragment
import cash.z.wallet.sdk.demoapp.databinding.FragmentGetAddressBinding

class GetAddressFragment : BaseDemoFragment<FragmentGetAddressBinding>() {

    private val config = App.instance.defaultConfig
    private var seed: ByteArray = config.seed
    private val initializer: Initializer = Initializer(App.instance, host = config.host, port = config.port)
    private val birthday = config.newWalletBirthday()

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetAddressBinding
            = FragmentGetAddressBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        /**
         * Create and initialize the wallet. Initialization will return the private keys but for the
         * purposes of this demo we don't need them.
         */
        initializer.new(seed, birthday)
    }

    override fun onResetComplete() {
        binding.textInfo.text = initializer.rustBackend.getAddress()
    }

    override fun onClear() {
        initializer.clear()
    }
}