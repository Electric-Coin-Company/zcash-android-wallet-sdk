package cash.z.wallet.sdk.demoapp.demos.getprivatekey

import android.view.LayoutInflater
import cash.z.wallet.sdk.demoapp.App
import cash.z.wallet.sdk.demoapp.BaseDemoFragment
import cash.z.wallet.sdk.demoapp.databinding.FragmentGetPrivateKeyBinding
import cash.z.wallet.sdk.secure.Wallet

class GetPrivateKeyFragment : BaseDemoFragment<FragmentGetPrivateKeyBinding>() {
    private var seed: ByteArray = App.instance.defaultConfig.seed
    private lateinit var wallet: Wallet
    private lateinit var privateKeys: Array<String>

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetPrivateKeyBinding =
        FragmentGetPrivateKeyBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        wallet = Wallet()
        
        /*
         * Initialize with the seed and retrieve one private key for each account specified (by
         * default, only 1 account is created). In a normal circumstance, a wallet app would then
         * store these keys in its secure storage for retrieval, later. Private keys are only needed
         * for sending funds.
         * 
         * Since we always clear the wallet, this function call will never return null. Otherwise, we
         * would interpret the null case to mean that the wallet data files already exist and
         * the private keys were stored externally (i.e. stored securely by the app, not the SDK).
         */
        privateKeys = wallet.initialize(App.instance, seed)!! 
    }

    override fun onResetComplete() {
        binding.textInfo.text = privateKeys[0]
    }

    override fun onClear() {
        wallet.clear()
    }

}