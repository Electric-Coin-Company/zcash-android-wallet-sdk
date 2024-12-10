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
    account: Account,
    send: ZecSend
) = createProposedTransactions(
    proposal =
        proposeTransfer(
            account = account,
            recipient = send.destination.address,
            amount = send.amount,
            memo = send.memo.value
        ),
    usk = spendingKey
)

/**
 * This is just a syntactic sugar function for [Synchronizer.proposeTransfer]
 */
suspend fun Synchronizer.proposeSend(
    account: Account,
    send: ZecSend
) = proposeTransfer(
    account,
    send.destination.address,
    send.amount,
    send.memo.value
)
