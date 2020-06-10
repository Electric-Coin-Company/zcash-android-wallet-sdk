[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk](../index.md) / [SdkSynchronizer](index.md) / [start](./start.md)

# start

`fun start(parentScope: CoroutineScope?): `[`Synchronizer`](../-synchronizer/index.md)

Starts this synchronizer within the given scope. For simplicity, attempting to start an
instance that has already been started will throw a [SynchronizerException.FalseStart](../../cash.z.ecc.android.sdk.exception/-synchronizer-exception/-false-start.md)
exception. This reduces the complexity of managing resources that must be recycled. Instead,
each synchronizer is designed to have a long lifespan and should be started from an activity,
application or session.

### Parameters

`parentScope` - the scope to use for this synchronizer, typically something with a
lifecycle such as an Activity for single-activity apps or a logged in user session. This
scope is only used for launching this synchronizer's job as a child. If no scope is provided,
then this synchronizer and all of its coroutines will run until stop is called, which is not
recommended since it can leak resources. That type of behavior is more useful for tests.

**Return**
an instance of this class so that this function can be used fluidly.

