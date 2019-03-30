[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.secure](../index.md) / [Wallet](index.md) / [fetchParams](./fetch-params.md)

# fetchParams

`suspend fun fetchParams(destinationDir: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Download and store the params into the given directory.

### Parameters

`destinationDir` - the directory where the params will be stored. It's assumed that we have write access to
this directory. Typically, this should be the app's cache directory because it is not harmful if these files are
cleared by the user since they are downloaded on-demand.