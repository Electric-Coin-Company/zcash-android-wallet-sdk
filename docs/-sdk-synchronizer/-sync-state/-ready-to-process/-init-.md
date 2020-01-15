[zcash-android-wallet-sdk](../../../../index.md) / [cash.z.wallet.sdk.data](../../../index.md) / [SdkSynchronizer](../../index.md) / [SyncState](../index.md) / [ReadyToProcess](index.md) / [&lt;init&gt;](./-init-.md)

# &lt;init&gt;

`ReadyToProcess(startingBlockHeight: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = Int.MAX_VALUE)`

The final state of the Synchronizer, when all initialization is complete and the starting block is known.

### Parameters

`startingBlockHeight` - the height that will be fed to the downloader. In most cases, it will represent
either the wallet birthday or the last block that was processed in the previous session.