[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk](../index.md) / [SdkSynchronizer](index.md) / [sendToAddress](./send-to-address.md)

# sendToAddress

`fun sendToAddress(spendingKey: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, zatoshi: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fromAccountIndex: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): Flow<`[`PendingTransaction`](../../cash.z.ecc.android.sdk.db.entity/-pending-transaction/index.md)`>`

Sends zatoshi.

### Parameters

`spendingKey` - the key associated with the notes that will be spent.

`zatoshi` - the amount of zatoshi to send.

`toAddress` - the recipient's address.

`memo` - the optional memo to include as part of the transaction.

`fromAccountIndex` - the optional account id to use. By default, the first account is used.

**Return**
a flow of PendingTransaction objects representing changes to the state of the
transaction. Any time the state changes a new instance will be emitted by this flow. This is
useful for updating the UI without needing to poll. Of course, polling is always an option
for any wallet that wants to ignore this return value.

