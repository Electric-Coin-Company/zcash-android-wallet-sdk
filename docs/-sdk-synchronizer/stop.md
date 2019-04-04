[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [SdkSynchronizer](index.md) / [stop](./stop.md)

# stop

`fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Overrides [Synchronizer.stop](../-synchronizer/stop.md)

Stops this synchronizer by stopping the downloader, repository, and activeTransactionManager, then cancelling the
parent job. Note that we do not cancel the parent scope that was passed into [start](start.md) because the synchronizer
does not own that scope, it just uses it for launching children.

