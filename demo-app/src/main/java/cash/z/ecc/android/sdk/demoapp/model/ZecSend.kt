package cash.z.ecc.android.sdk.demoapp.model

import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.Zatoshi

data class ZecSend(val destination: WalletAddress, val amount: Zatoshi, val memo: Memo) {
    companion object
}

fun Synchronizer.send(spendingKey: UnifiedSpendingKey, send: ZecSend) = sendToAddress(
    spendingKey,
    send.amount,
    send.destination.address,
    send.memo.value
)
