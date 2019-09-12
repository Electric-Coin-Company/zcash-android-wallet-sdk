[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [SdkSynchronizer](index.md) / [pendingTransactions](./pending-transactions.md)

# pendingTransactions

`fun pendingTransactions(): ReceiveChannel<`[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`PendingTransaction`](../../cash.z.wallet.sdk.entity/-pending-transaction/index.md)`>>`

Overrides [Synchronizer.pendingTransactions](../-synchronizer/pending-transactions.md)

A stream of all the outbound pending transaction that have been sent but are awaiting confirmations.

