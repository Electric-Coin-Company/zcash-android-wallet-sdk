[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk](../index.md) / [SdkSynchronizer](index.md) / [stop](./stop.md)

# stop

`fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Overrides [Synchronizer.stop](../-synchronizer/stop.md)

Stop this synchronizer and all of its child jobs. Once a synchronizer has been stopped it
should not be restarted and attempting to do so will result in an error. Also, this function
will throw an exception if the synchronizer was never previously started.

