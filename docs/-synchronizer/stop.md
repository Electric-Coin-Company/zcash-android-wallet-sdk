[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk](../index.md) / [Synchronizer](index.md) / [stop](./stop.md)

# stop

`abstract fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Stop this synchronizer. Implementations should ensure that calling this method cancels all
jobs that were created by this instance.

Note that in most cases, there is no need to call [stop](./stop.md) because the Synchronizer will
automatically stop whenever the parentScope is cancelled. For instance, if that scope is
bound to the lifecycle of the activity, the Synchronizer will stop when the activity stops.
However, if no scope is provided to the start method, then the Synchronizer must be stopped
with this function.

