[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk](../index.md) / [Synchronizer](index.md) / [start](./start.md)

# start

`abstract fun start(parentScope: CoroutineScope? = null): `[`Synchronizer`](index.md)

Starts this synchronizer within the given scope.

### Parameters

`parentScope` - the scope to use for this synchronizer, typically something with a
lifecycle such as an Activity. Implementations should leverage structured concurrency and
cancel all jobs when this scope completes.