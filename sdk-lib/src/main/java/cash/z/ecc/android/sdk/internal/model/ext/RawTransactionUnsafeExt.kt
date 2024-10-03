package cash.z.ecc.android.sdk.internal.model.ext

import cash.z.ecc.android.sdk.internal.model.TransactionStatus
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe.MainChain
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe.Mempool
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe.OrphanedBlock

internal fun RawTransactionUnsafe.toTransactionStatus(): TransactionStatus {
    return when (this) {
        is MainChain -> TransactionStatus.Mined(height.toBlockHeight())
        is Mempool -> TransactionStatus.NotInMainChain
        is OrphanedBlock -> TransactionStatus.NotInMainChain
    }
}
