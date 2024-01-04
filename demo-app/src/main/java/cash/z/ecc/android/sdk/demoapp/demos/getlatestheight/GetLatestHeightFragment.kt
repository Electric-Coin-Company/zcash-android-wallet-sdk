package cash.z.ecc.android.sdk.demoapp.demos.getlatestheight

import android.view.LayoutInflater
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetLatestHeightBinding

/**
 * Retrieves the latest block height from the lightwalletd server. This is the simplest test for
 * connectivity with the server. Modify the `host` and the `port` inside of
 * `App.instance.defaultConfig` to check the SDK's ability to communicate with a given lightwalletd
 * instance.
 */
class GetLatestHeightFragment : BaseDemoFragment<FragmentGetLatestHeightBinding>() {
    // TODO [#973]: Eliminate old UI demo-app
    // TODO [#973]: https://github.com/zcash/zcash-android-wallet-sdk/issues/973
    private fun displayLatestHeight() {
        // note: this is a blocking call, a real app wouldn't do this on the main thread
        //       instead, a production app would leverage the synchronizer like in the other demos
        // binding.textInfo.text = lightWalletClient?.getLatestBlockHeight().toString()
    }

    //
    // Android Lifecycle overrides
    //

    override fun onResume() {
        super.onResume()
        displayLatestHeight()
    }

    //
    // Base Fragment overrides
    //

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetLatestHeightBinding =
        FragmentGetLatestHeightBinding.inflate(layoutInflater)
}
