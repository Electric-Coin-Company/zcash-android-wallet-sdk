[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk.data](../index.md) / [SdkSynchronizer](index.md) / [isStale](./is-stale.md)

# isStale

`suspend fun isStale(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Overrides [Synchronizer.isStale](../-synchronizer/is-stale.md)

A flag to indicate that this Synchronizer is significantly out of sync with it's server. This is determined by
the delta between the current block height reported by the server and the latest block we have stored in cache.
Whenever this delta is greater than the [staleTolerance](#), this function returns true. This is intended for
showing progress indicators when the user returns to the app after having not used it for a long period.
Typically, this means the user may have to wait for downloading to occur and the current balance and transaction
information cannot be trusted as 100% accurate.

**Return**
true when the local data is significantly out of sync with the remote server and the app data is stale.

