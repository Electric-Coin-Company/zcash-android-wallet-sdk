[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [SdkSynchronizer](index.md) / [sendToAddress](./send-to-address.md)

# sendToAddress

`suspend fun sendToAddress(zatoshi: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fromAccountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Overrides [Synchronizer.sendToAddress](../-synchronizer/send-to-address.md)

Sends zatoshi.

### Parameters

`zatoshi` - the amount of zatoshi to send.

`toAddress` - the recipient's address.

`memo` - the optional memo to include as part of the transaction.

`fromAccountId` - the optional account id to use. By default, the first account is used.