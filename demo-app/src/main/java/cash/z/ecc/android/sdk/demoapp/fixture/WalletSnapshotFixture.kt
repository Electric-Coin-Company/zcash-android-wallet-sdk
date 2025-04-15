package cash.z.ecc.android.sdk.demoapp.fixture

import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel.SynchronizerError
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel.WalletSnapshot
import cash.z.ecc.android.sdk.fixture.AccountBalanceFixture
import cash.z.ecc.android.sdk.fixture.AccountFixture
import cash.z.ecc.android.sdk.model.AccountBalance
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.PercentDecimal
import java.math.BigDecimal

@Suppress("MagicNumber")
object WalletSnapshotFixture {
    val STATUS = Synchronizer.Status.SYNCED
    val PROGRESS = PercentDecimal.ZERO_PERCENT
    val IS_SPENDABLE = false
    val EXCHANGE_RATE_USD: BigDecimal = BigDecimal(37.4850)
    val ACCOUNT = AccountFixture.new()
    val WALLET_BALANCES: Map<AccountUuid, AccountBalance> =
        mapOf(
            ACCOUNT.accountUuid to AccountBalanceFixture.new()
        )

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
        walletBalances: Map<AccountUuid, AccountBalance> = WALLET_BALANCES,
        exchangeRateUsd: BigDecimal? = EXCHANGE_RATE_USD,
        progress: PercentDecimal = PROGRESS,
        isSpendable: Boolean = IS_SPENDABLE,
        synchronizerError: SynchronizerError? = null
    ) = WalletSnapshot(
        status = status,
        processorInfo = processorInfo,
        walletBalances = walletBalances,
        exchangeRateUsd = exchangeRateUsd,
        progress = progress,
        isSpendable = isSpendable,
        synchronizerError = synchronizerError
    )
}
