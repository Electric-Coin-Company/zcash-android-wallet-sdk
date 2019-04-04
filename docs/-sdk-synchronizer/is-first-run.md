[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [SdkSynchronizer](index.md) / [isFirstRun](./is-first-run.md)

# isFirstRun

`suspend fun isFirstRun(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Overrides [Synchronizer.isFirstRun](../-synchronizer/is-first-run.md)

A flag to indicate that the initial state of this synchronizer was firstRun. This is useful for knowing whether
initializing the database is required and whether to show things like"first run walk-throughs."

**Return**
true when this synchronizer has not been run before on this device or when cache has been cleared since
the last run.

