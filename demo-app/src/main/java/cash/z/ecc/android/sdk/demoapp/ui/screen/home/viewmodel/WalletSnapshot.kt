package cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel

import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountBalance
import cash.z.ecc.android.sdk.model.PercentDecimal
import java.math.BigDecimal

data class WalletSnapshot(
    val status: Synchronizer.Status,
    val processorInfo: CompactBlockProcessor.ProcessorInfo,
    val walletBalances: Map<Account, AccountBalance>,
    val exchangeRateUsd: BigDecimal?,
    val progress: PercentDecimal,
    val synchronizerError: SynchronizerError?
) {
    fun balanceByAccount(account: Account): AccountBalance {
        return walletBalances[account] ?: error("Balance of $account could not be find.")
    }
}
