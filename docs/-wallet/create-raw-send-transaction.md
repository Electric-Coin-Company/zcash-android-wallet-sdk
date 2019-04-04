[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.secure](../index.md) / [Wallet](index.md) / [createRawSendTransaction](./create-raw-send-transaction.md)

# createRawSendTransaction

`suspend fun createRawSendTransaction(value: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = "", fromAccountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = accountIds[0]): `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)

Does the proofs and processing required to create a raw transaction and inserts the result in the database. On
average, this call takes over 10 seconds.

### Parameters

`value` - the zatoshi value to send

`toAddress` - the destination address

`memo` - the memo, which is not augmented in any way

**Return**
the row id in the transactions table that contains the raw transaction or -1 if it failed

