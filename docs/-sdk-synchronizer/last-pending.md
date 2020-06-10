[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk.data](../index.md) / [SdkSynchronizer](index.md) / [lastPending](./last-pending.md)

# lastPending

`fun lastPending(): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`PendingTransaction`](../../cash.z.ecc.android.sdk.entity/-pending-transaction/index.md)`>`

Overrides [Synchronizer.lastPending](../-synchronizer/last-pending.md)

Holds the most recent value that was transmitted through the [pendingTransactions](../-synchronizer/pending-transactions.md) channel. Typically, if the
underlying channel is a BroadcastChannel (and it should be),then this value is simply [pendingChannel.value](#)

