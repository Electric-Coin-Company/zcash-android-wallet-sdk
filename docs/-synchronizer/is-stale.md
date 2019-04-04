[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [Synchronizer](index.md) / [isStale](./is-stale.md)

# isStale

`abstract suspend fun isStale(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

A flag to indicate that this Synchronizer is significantly out of sync with it's server. Typically, this means
that the balance and other data cannot be completely trusted because a significant amount of data has not been
processed. This is intended for showing progress indicators when the user returns to the app after having not
used it for days. Typically, this means minor sync issues should be ignored and this should be leveraged in order
to alert a user that the balance information is stale.

**Return**
true when the local data is significantly out of sync with the remote server and the app data is stale.

