[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [MockSynchronizer](index.md) / [start](./start.md)

# start

`open fun start(parentScope: CoroutineScope): `[`Synchronizer`](../-synchronizer/index.md)

Overrides [Synchronizer.start](../-synchronizer/start.md)

Starts this synchronizer within the given scope.

### Parameters

`parentScope` - the scope to use for this synchronizer, typically something with a lifecycle such as an
Activity. Implementations should leverage structured concurrency and cancel all jobs when this scope completes.