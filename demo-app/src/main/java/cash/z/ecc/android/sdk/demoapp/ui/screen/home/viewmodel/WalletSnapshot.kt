package cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel

import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor
import cash.z.ecc.android.sdk.model.AccountBalance
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.PercentDecimal
import java.math.BigDecimal

data class WalletSnapshot(
    val status: Synchronizer.Status,
    val processorInfo: CompactBlockProcessor.ProcessorInfo,
    val walletBalances: Map<AccountUuid, AccountBalance>,
    val exchangeRateUsd: BigDecimal?,
    val progress: PercentDecimal,
    val recoveryProgress: PercentDecimal,
    val synchronizerError: SynchronizerError?
) {
    fun balanceByAccountUuid(accountUuid: AccountUuid): AccountBalance =
        walletBalances[accountUuid] ?: error("Balance of account? $accountUuid could not be found.")
}
