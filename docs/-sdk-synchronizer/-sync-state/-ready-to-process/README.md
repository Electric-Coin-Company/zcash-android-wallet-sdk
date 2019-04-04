[zcash-android-wallet-sdk](../../../../index.md) / [cash.z.wallet.sdk.data](../../../index.md) / [SdkSynchronizer](../../index.md) / [SyncState](../index.md) / [ReadyToProcess](./index.md)

# ReadyToProcess

`class ReadyToProcess : `[`SdkSynchronizer.SyncState`](../index.md)

The final state of the Synchronizer, when all initialization is complete and the starting block is known.

### Parameters

`startingBlockHeight` - the height that will be fed to the downloader. In most cases, it will represent
either the wallet birthday or the last block that was processed in the previous session.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `ReadyToProcess(startingBlockHeight: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = Int.MAX_VALUE)`<br>The final state of the Synchronizer, when all initialization is complete and the starting block is known. |

### Properties

| Name | Summary |
|---|---|
| [startingBlockHeight](starting-block-height.md) | `val startingBlockHeight: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>the height that will be fed to the downloader. In most cases, it will represent either the wallet birthday or the last block that was processed in the previous session. |
