[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [Synchronizer](index.md) / [lastPending](./last-pending.md)

# lastPending

`abstract fun lastPending(): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`PendingTransaction`](../../cash.z.wallet.sdk.entity/-pending-transaction/index.md)`>`

Holds the most recent value that was transmitted through the [pendingTransactions](pending-transactions.md) channel. Typically, if the
underlying channel is a BroadcastChannel (and it should be),then this value is simply [pendingChannel.value](#)

