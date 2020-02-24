package cash.z.wallet.sdk.demoapp.demos.getprivatekey

import android.view.LayoutInflater
import cash.z.wallet.sdk.Initializer
import cash.z.wallet.sdk.demoapp.App
import cash.z.wallet.sdk.demoapp.BaseDemoFragment
import cash.z.wallet.sdk.demoapp.databinding.FragmentGetPrivateKeyBinding

class GetPrivateKeyFragment : BaseDemoFragment<FragmentGetPrivateKeyBinding>() {
    private var seed: ByteArray = App.instance.defaultConfig.seed
    private val initializer: Initializer = Initializer(App.instance)
    private val birthday = App.instance.defaultConfig.newWalletBirthday()
    private lateinit var spendingKeys: Array<String>
    private lateinit var viewingKeys: Array<String>

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetPrivateKeyBinding =
        FragmentGetPrivateKeyBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        /*
         * Initialize with the seed and retrieve one private key for each account specified (by
         * default, only 1 account is created). In a normal circumstance, a wallet app would then
         * store these keys in its secure storage for retrieval, later. Private keys are only needed
         * for sending funds.
         */
        spendingKeys = initializer.new(seed, birthday)

        /*
         * Viewing keys can be derived from a seed or from spending keys.
         */
        viewingKeys = initializer.deriveViewingKeys(seed)

        // just for demonstration purposes to show that these approaches produce the same result.
        require(spendingKeys.first() == initializer.deriveSpendingKeys(seed).first())
        require(viewingKeys.first() == initializer.deriveViewingKey(spendingKeys.first()))
    }

    override fun onResetComplete() {
        binding.textInfo.text = "Spending Key:\n${spendingKeys[0]}\n\nViewing Key:\n${viewingKeys[0]}"
    }

    override fun onClear() {
        initializer.clear()
    }

}