package cash.z.ecc.android.sdk.demoapp.demos.getprivatekey

import android.view.LayoutInflater
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.demoapp.App
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetPrivateKeyBinding

/**
 * Displays the viewing key and spending key associated with the seed defined by the default config.
 * To modify the seed that is used, update the `DemoConfig.seedWords` value. This demo takes two
 * approaches to deriving the seed, one that is stateless and another that is not. In most cases, a
 * wallet instance will call `new` on an initializer and then store the result.
 */
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
         * store these keys in its secure storage for retrieval, later. Spending keys are only
         * needed when sending funds. Viewing keys can be derived from spending keys. In most cases,
         * a call to `initializer.new` or `initializer.import` are the only time a wallet passes the
         * seed to the SDK. From that point forward, only spending or viewing keys are needed.
         */
        spendingKeys = initializer.new(seed, birthday)

        /*
         * Alternatively, viewing keys can also be derived directly from a seed or spending keys.
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
