package cash.z.wallet.sdk.demoapp.demos.getaddress

import android.view.LayoutInflater
import cash.z.wallet.sdk.demoapp.App
import cash.z.wallet.sdk.demoapp.BaseDemoFragment
import cash.z.wallet.sdk.demoapp.databinding.FragmentGetAddressBinding
import cash.z.wallet.sdk.secure.Wallet

class GetAddressFragment : BaseDemoFragment<FragmentGetAddressBinding>() {

    private var seed: ByteArray = App.instance.defaultConfig.seed
    private lateinit var wallet: Wallet

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetAddressBinding
            = FragmentGetAddressBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        /**
         * Create and initialize the wallet. Initialization will return the private keys but for the
         * purposes of this demo we don't need them.
         */
        wallet = Wallet().also {
            it.initialize(App.instance, seed)
        }
    }

    override fun onResetComplete() {
        binding.textInfo.text = wallet.getAddress()
    }

    override fun onClear() {
        wallet.clear()
    }
}