[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [MockSynchronizer](index.md) / [isStale](./is-stale.md)

# isStale

`open suspend fun isStale(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Overrides [Synchronizer.isStale](../-synchronizer/is-stale.md)

Returns true roughly 10% of the time and then resets to false after some delay.

