package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.Synchronizer

data class ZecSend(val destination: WalletAddress, val amount: Zatoshi, val memo: Memo) {
    companion object
}

suspend fun Synchronizer.send(
    spendingKey: UnifiedSpendingKey,
    send: ZecSend
) = createProposedTransactions(
    proposeTransfer(
        spendingKey.account,
        send.destination.address,
        send.amount,
        send.memo.value
    ),
    spendingKey
)

suspend fun Synchronizer.proposeSend(
    spendingKey: UnifiedSpendingKey,
    send: ZecSend
) = proposeTransfer(
    spendingKey.account,
    send.destination.address,
    send.amount,
    send.memo.value
)
