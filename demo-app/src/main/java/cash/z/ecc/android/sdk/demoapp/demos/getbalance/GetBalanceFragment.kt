package cash.z.ecc.android.sdk.demoapp.demos.getbalance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetBalanceBinding
import cash.z.ecc.android.sdk.demoapp.ext.requireApplicationContext
import cash.z.ecc.android.sdk.demoapp.util.SyncBlockchainBenchmarkTrace
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.ext.convertZatoshiToZec
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.toUsdString
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.PercentDecimal
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.DerivationTool
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * Displays the available balance && total balance associated with the seed defined by the default config.
 * comments.
 */
@Suppress("TooManyFunctions")
class GetBalanceFragment : BaseDemoFragment<FragmentGetBalanceBinding>() {
    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetBalanceBinding =
        FragmentGetBalanceBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reportTraceEvent(SyncBlockchainBenchmarkTrace.Event.BALANCE_SCREEN_START)
    }

    override fun onDestroy() {
        super.onDestroy()
        reportTraceEvent(SyncBlockchainBenchmarkTrace.Event.BALANCE_SCREEN_END)
    }

    @Suppress("MagicNumber")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val seedPhrase = sharedViewModel.seedPhrase.value
        val seed = Mnemonics.MnemonicCode(seedPhrase).toSeed()
        val network = ZcashNetwork.fromResources(requireApplicationContext())

        binding.refreshExchangeRate.apply {
            setOnClickListener {
                lifecycleScope.launch {
                    sharedViewModel.synchronizerFlow.value?.let { synchronizer ->
                        synchronizer.refreshExchangeRateUsd()
                    }
                }
            }
        }

        binding.shield.apply {
            setOnClickListener {
                lifecycleScope.launch {
                    val usk =
                        DerivationTool.getInstance().deriveUnifiedSpendingKey(
                            seed,
                            network,
                            Account.DEFAULT
                        )
                    sharedViewModel.synchronizerFlow.value?.let { synchronizer ->
                        synchronizer.proposeShielding(usk.account, Zatoshi(100000))?.let { it1 ->
                            synchronizer.createProposedTransactions(
                                it1,
                                usk
                            )
                        }
                    }
                }
            }
        }

        monitorChanges()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun monitorChanges() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.status }
                        .collect { onStatus(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.progress }
                        .collect { onProgress(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest {
                            it.saplingBalances.combine(it.exchangeRateUsd) { b, r ->
                                b?.let { Pair(b, r?.first) }
                            }
                        }
                        .collect { onSaplingBalance(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest {
                            it.orchardBalances.combine(it.exchangeRateUsd) { b, r ->
                                b?.let { Pair(b, r?.first) }
                            }
                        }
                        .collect { onOrchardBalance(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest {
                            it.transparentBalance.combine(it.exchangeRateUsd) { b, r ->
                                b?.let { Pair(b, r?.first) }
                            }
                        }
                        .collect { onTransparentBalance(it) }
                }
            }
        }
    }

    private fun onOrchardBalance(orchardBalance: Pair<WalletBalance, BigDecimal?>?) {
        binding.orchardBalance.apply {
            text = orchardBalance.balanceHumanString()
        }
    }

    private fun onSaplingBalance(saplingBalance: Pair<WalletBalance, BigDecimal?>?) {
        binding.saplingBalance.apply {
            text = saplingBalance.balanceHumanString()
        }
    }

    private fun onTransparentBalance(transparentBalance: Pair<Zatoshi, BigDecimal?>?) {
        binding.transparentBalance.apply {
            text = transparentBalance.humanString()
        }

        binding.shield.apply {
            // This check is not entirely correct - it does not calculate the resulting fee with the new Proposal API
            // Note that the entire fragment-based old Demo app will be removed as part of [#973]
            visibility =
                if ((transparentBalance?.first ?: Zatoshi(0)).value > 0L) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }

    private fun onStatus(status: Synchronizer.Status) {
        Twig.debug { "Synchronizer status: $status" }
        // report benchmark event
        val traceEvents =
            when (status) {
                Synchronizer.Status.SYNCING -> {
                    SyncBlockchainBenchmarkTrace.Event.BLOCKCHAIN_SYNC_START
                }

                Synchronizer.Status.SYNCED -> {
                    SyncBlockchainBenchmarkTrace.Event.BLOCKCHAIN_SYNC_END
                }

                else -> null
            }
        traceEvents?.let { reportTraceEvent(it) }

        binding.textStatus.text = "Status: $status"
        sharedViewModel.synchronizerFlow.value?.let { synchronizer ->
            val rate = synchronizer.exchangeRateUsd.value?.first
            onOrchardBalance(synchronizer.orchardBalances.value?.let { Pair(it, rate) })
            onSaplingBalance(synchronizer.saplingBalances.value?.let { Pair(it, rate) })
            onTransparentBalance(synchronizer.transparentBalance.value?.let { Pair(it, rate) })
        }
    }

    @Suppress("MagicNumber")
    private fun onProgress(percent: PercentDecimal) {
        if (percent.isLessThanHundredPercent()) {
            binding.textStatus.text = "Syncing blocks...${percent.toPercentage()}%"
        }
    }
}

@Suppress("MagicNumber")
private fun Pair<WalletBalance, BigDecimal?>?.balanceHumanString() =
    if (null == this) {
        "Calculating balance"
    } else {
        """
        Pending balance: ${first.pending.convertZatoshiToZecString(12)} (${
            second?.multiply(first.pending.convertZatoshiToZec())
                .toUsdString()
        } USD)
        Available balance: ${first.available.convertZatoshiToZecString(12)} (${
            second?.multiply(first.available.convertZatoshiToZec())
                .toUsdString()
        } USD)
        Total balance: ${first.total.convertZatoshiToZecString(12)} (${
            second?.multiply(first.total.convertZatoshiToZec())
                .toUsdString()
        } USD)
        """.trimIndent()
    }

@Suppress("MagicNumber")
private fun Pair<Zatoshi, BigDecimal?>?.humanString() =
    if (null == this) {
        "Calculating balance"
    } else {
        """
        Balance: ${first.convertZatoshiToZecString(12)} (${
            second?.multiply(first.convertZatoshiToZec())
                .toUsdString()
        } USD)
        """.trimIndent()
    }
