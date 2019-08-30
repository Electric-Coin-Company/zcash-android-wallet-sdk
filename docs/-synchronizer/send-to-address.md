[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [Synchronizer](index.md) / [sendToAddress](./send-to-address.md)

# sendToAddress

`abstract suspend fun sendToAddress(zatoshi: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = "", fromAccountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 0): `[`PendingTransaction`](../../cash.z.wallet.sdk.entity/-pending-transaction/index.md)

Sends zatoshi.

### Parameters

`zatoshi` - the amount of zatoshi to send.

`toAddress` - the recipient's address.

`memo` - the optional memo to include as part of the transaction.

`fromAccountId` - the optional account id to use. By default, the first account is used.