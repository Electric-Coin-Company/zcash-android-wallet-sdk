[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [SdkSynchronizer](index.md) / [start](./start.md)

# start

`fun start(parentScope: CoroutineScope): `[`Synchronizer`](../-synchronizer/index.md)

Overrides [Synchronizer.start](../-synchronizer/start.md)

Starts this synchronizer within the given scope. For simplicity, attempting to start an instance that has already
been started will throw a [SynchronizerException.FalseStart](../../cash.z.wallet.sdk.exception/-synchronizer-exception/-false-start.md) exception. This reduces the complexity of managing
resources that must be recycled. Instead, each synchronizer is designed to have a long lifespan (similar to act or application) &lt;=- explain usage

### Parameters

`parentScope` - the scope to use for this synchronizer, typically something with a lifecycle such as an
Activity. Implementations should leverage structured concurrency and cancel all jobs when this scope completes.