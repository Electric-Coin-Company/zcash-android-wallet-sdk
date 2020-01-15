[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk](../index.md) / [SdkSynchronizer](index.md) / [pendingTransactions](./pending-transactions.md)

# pendingTransactions

`val pendingTransactions: Flow<`[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`PendingTransaction`](../../cash.z.wallet.sdk.entity/-pending-transaction/index.md)`>>`

Overrides [Synchronizer.pendingTransactions](../-synchronizer/pending-transactions.md)

A flow of all the outbound pending transaction that have been sent but are awaiting
confirmations.

