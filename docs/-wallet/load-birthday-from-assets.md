[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.secure](../index.md) / [Wallet](index.md) / [loadBirthdayFromAssets](./load-birthday-from-assets.md)

# loadBirthdayFromAssets

`fun loadBirthdayFromAssets(context: Context, birthdayHeight: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`? = null): `[`Wallet.WalletBirthday`](-wallet-birthday/index.md)

Load the given birthday file from the assets of the given context. When no height is specified, we default to
the file with the greatest name.

### Parameters

`context` - the context from which to load assets.

`birthdayHeight` - the height file to look for among the file names.

**Return**
a WalletBirthday that reflects the contents of the file or an exception when parsing fails.

