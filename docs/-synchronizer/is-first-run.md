[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [Synchronizer](index.md) / [isFirstRun](./is-first-run.md)

# isFirstRun

`abstract suspend fun isFirstRun(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

A flag to indicate that this is the first run of this Synchronizer on this device. This is useful for knowing
whether to initialize databases or other required resources, as well as whether to show walk-throughs.

**Return**
true when this is the first run. Implementations can set criteria for that but typically it will be when
the database needs to be initialized.

