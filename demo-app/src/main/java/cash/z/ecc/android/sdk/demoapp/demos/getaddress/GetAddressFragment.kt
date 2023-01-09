package cash.z.ecc.android.sdk.demoapp.demos.getaddress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetAddressBinding
import cash.z.ecc.android.sdk.demoapp.ext.requireApplicationContext
import cash.z.ecc.android.sdk.demoapp.type.fromResources
import cash.z.ecc.android.sdk.demoapp.util.ProvideAddressBenchmarkTrace
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.UnifiedFullViewingKey
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Displays the address associated with the seed defined by the default config. To modify the seed
 * that is used, update the `DemoConfig.seedWords` value.
 */
class GetAddressFragment : BaseDemoFragment<FragmentGetAddressBinding>() {

    private lateinit var viewingKey: UnifiedFullViewingKey

    private fun displayAddress() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.synchronizerFlow.filterNotNull().collect { synchronizer ->
                        binding.unifiedAddress.apply {
                            reportTraceEvent(ProvideAddressBenchmarkTrace.Event.UNIFIED_ADDRESS_START)
                            val uaddress = synchronizer.getUnifiedAddress()
                            reportTraceEvent(ProvideAddressBenchmarkTrace.Event.UNIFIED_ADDRESS_END)
                            text = uaddress
                            setOnClickListener { copyToClipboard(uaddress) }
                        }
                        binding.saplingAddress.apply {
                            reportTraceEvent(ProvideAddressBenchmarkTrace.Event.SAPLING_ADDRESS_START)
                            val sapling = synchronizer.getSaplingAddress()
                            reportTraceEvent(ProvideAddressBenchmarkTrace.Event.SAPLING_ADDRESS_END)
                            text = sapling
                            setOnClickListener { copyToClipboard(sapling) }
                        }
                        binding.transparentAddress.apply {
                            reportTraceEvent(ProvideAddressBenchmarkTrace.Event.TRANSPARENT_ADDRESS_START)
                            val transparent = synchronizer.getTransparentAddress()
                            reportTraceEvent(ProvideAddressBenchmarkTrace.Event.TRANSPARENT_ADDRESS_END)
                            text = transparent
                            setOnClickListener { copyToClipboard(transparent) }
                        }
                    }
                }
            }
        }
    }

    //
    // Android Lifecycle overrides
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reportTraceEvent(ProvideAddressBenchmarkTrace.Event.ADDRESS_SCREEN_START)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayAddress()
    }

    override fun onDestroy() {
        super.onDestroy()
        reportTraceEvent(ProvideAddressBenchmarkTrace.Event.ADDRESS_SCREEN_END)
    }

    //
    // Base Fragment overrides
    //

    override fun onActionButtonClicked() {
        viewLifecycleOwner.lifecycleScope.launch {
            copyToClipboard(
                DerivationTool.deriveUnifiedAddress(
                    viewingKey.encoding,
                    ZcashNetwork.fromResources(requireApplicationContext())
                ),
                "Unified address copied to clipboard!"
            )
        }
    }

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetAddressBinding =
        FragmentGetAddressBinding.inflate(layoutInflater)
}
