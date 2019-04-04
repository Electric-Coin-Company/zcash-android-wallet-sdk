[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [MockSynchronizer](index.md) / [sendToAddress](./send-to-address.md)

# sendToAddress

`open suspend fun sendToAddress(zatoshi: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fromAccountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Overrides [Synchronizer.sendToAddress](../-synchronizer/send-to-address.md)

Uses the [forge](#) to fabricate a transaction and then walk it through the transaction lifecycle in a useful way.
This method will validate the zatoshi amount and toAddress a bit to help with UI validation.

### Parameters

`zatoshi` - the amount to send. A transaction will be created matching this amount.

`toAddress` - the address to use. An active transaction will be created matching this address.

`memo` - the memo to use. This field is ignored.

`fromAccountId` - the account. This field is ignored.