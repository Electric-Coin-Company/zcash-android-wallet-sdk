[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk](../index.md) / [Synchronizer](index.md) / [sendToAddress](./send-to-address.md)

# sendToAddress

`abstract fun sendToAddress(spendingKey: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, zatoshi: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = "", fromAccountIndex: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 0): Flow<`[`PendingTransaction`](../../cash.z.wallet.sdk.entity/-pending-transaction/index.md)`>`

Sends zatoshi.

### Parameters

`spendingKey` - the key that allows spends to occur.

`zatoshi` - the amount of zatoshi to send.

`toAddress` - the recipient's address.

`memo` - the optional memo to include as part of the transaction.

`fromAccountId` - the optional account id to use. By default, the first account is used.