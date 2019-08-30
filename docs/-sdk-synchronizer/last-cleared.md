[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [SdkSynchronizer](index.md) / [lastCleared](./last-cleared.md)

# lastCleared

`fun lastCleared(): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`ClearedTransaction`](../../cash.z.wallet.sdk.entity/-cleared-transaction/index.md)`>`

Overrides [Synchronizer.lastCleared](../-synchronizer/last-cleared.md)

Holds the most recent value that was transmitted through the [clearedTransactions](../-synchronizer/cleared-transactions.md) channel. Typically, if the
underlying channel is a BroadcastChannel (and it should be), then this value is simply [clearedChannel.value](#)

