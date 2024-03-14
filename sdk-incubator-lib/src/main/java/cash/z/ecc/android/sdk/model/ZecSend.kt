package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.Synchronizer

data class ZecSend(
    val destination: WalletAddress,
    val amount: Zatoshi,
    val memo: Memo,
    val proposal: Proposal?
) {
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

/**
 * This is just a syntactic sugar function for [Synchronizer.proposeTransfer]
 */
suspend fun Synchronizer.proposeSend(
    spendingKey: UnifiedSpendingKey,
    send: ZecSend
) = proposeTransfer(
    spendingKey.account,
    send.destination.address,
    send.amount,
    send.memo.value
)
