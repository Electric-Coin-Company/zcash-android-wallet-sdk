package cash.z.ecc.android.sdk.demoapp.fixture

import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel.SynchronizerError
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel.WalletSnapshot
import cash.z.ecc.android.sdk.fixture.WalletBalanceFixture
import cash.z.ecc.android.sdk.model.PercentDecimal
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import java.math.BigDecimal

@Suppress("MagicNumber")
object WalletSnapshotFixture {
    val STATUS = Synchronizer.Status.SYNCED
    val PROGRESS = PercentDecimal.ZERO_PERCENT
    val TRANSPARENT_BALANCE: Zatoshi = Zatoshi(8)
    val ORCHARD_BALANCE: WalletBalance = WalletBalanceFixture.new(Zatoshi(5), Zatoshi(2), Zatoshi(1))
    val SAPLING_BALANCE: WalletBalance = WalletBalanceFixture.new(Zatoshi(4), Zatoshi(4), Zatoshi(2))
    val EXCHANGE_RATE_USD: BigDecimal = BigDecimal(37.4850)

    // Should fill in with non-empty values for better example values in tests and UI previews
    @Suppress("LongParameterList")
    fun new(
        status: Synchronizer.Status = STATUS,
        processorInfo: CompactBlockProcessor.ProcessorInfo =
            CompactBlockProcessor.ProcessorInfo(
                null,
                null,
                null
            ),
        orchardBalance: WalletBalance = ORCHARD_BALANCE,
        saplingBalance: WalletBalance = SAPLING_BALANCE,
        transparentBalance: Zatoshi = TRANSPARENT_BALANCE,
        exchangeRateUsd: BigDecimal? = EXCHANGE_RATE_USD,
        progress: PercentDecimal = PROGRESS,
        synchronizerError: SynchronizerError? = null
    ) = WalletSnapshot(
        status,
        processorInfo,
        orchardBalance,
        saplingBalance,
        transparentBalance,
        exchangeRateUsd,
        progress,
        synchronizerError
    )
}
