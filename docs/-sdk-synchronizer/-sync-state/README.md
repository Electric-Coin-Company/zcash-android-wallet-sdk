[zcash-android-wallet-sdk](../../../index.md) / [cash.z.wallet.sdk.data](../../index.md) / [SdkSynchronizer](../index.md) / [SyncState](./index.md)

# SyncState

`sealed class SyncState`

Represents the initial state of the Synchronizer.

### Types

| Name | Summary |
|---|---|
| [CacheOnly](-cache-only/index.md) | `class CacheOnly : `[`SdkSynchronizer.SyncState`](./index.md)<br>State for when compact blocks have been downloaded but not scanned. This state is typically achieved when the app was previously started but killed before the first scan took place. In this case, we do not need to download compact blocks that we already have. |
| [FirstRun](-first-run.md) | `object FirstRun : `[`SdkSynchronizer.SyncState`](./index.md)<br>State for the first run of the Synchronizer, when the database has not been initialized. |
| [ReadyToProcess](-ready-to-process/index.md) | `class ReadyToProcess : `[`SdkSynchronizer.SyncState`](./index.md)<br>The final state of the Synchronizer, when all initialization is complete and the starting block is known. |

### Inheritors

| Name | Summary |
|---|---|
| [CacheOnly](-cache-only/index.md) | `class CacheOnly : `[`SdkSynchronizer.SyncState`](./index.md)<br>State for when compact blocks have been downloaded but not scanned. This state is typically achieved when the app was previously started but killed before the first scan took place. In this case, we do not need to download compact blocks that we already have. |
| [FirstRun](-first-run.md) | `object FirstRun : `[`SdkSynchronizer.SyncState`](./index.md)<br>State for the first run of the Synchronizer, when the database has not been initialized. |
| [ReadyToProcess](-ready-to-process/index.md) | `class ReadyToProcess : `[`SdkSynchronizer.SyncState`](./index.md)<br>The final state of the Synchronizer, when all initialization is complete and the starting block is known. |
