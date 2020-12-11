package cash.z.ecc.android.sdk.demoapp.demos.getbalance

import android.view.LayoutInflater
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetBalanceBinding

class GetBalanceFragment : BaseDemoFragment<FragmentGetBalanceBinding>() {

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetBalanceBinding =
        FragmentGetBalanceBinding.inflate(layoutInflater)

}
