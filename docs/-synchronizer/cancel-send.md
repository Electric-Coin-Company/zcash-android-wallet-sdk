[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [Synchronizer](index.md) / [cancelSend](./cancel-send.md)

# cancelSend

`abstract fun cancelSend(transaction: `[`SentTransaction`](../../cash.z.wallet.sdk.entity/-sent-transaction/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Attempts to cancel a previously sent transaction. Typically, cancellation is only an option if the transaction
has not yet been submitted to the server.

### Parameters

`transaction` - the transaction to cancel.

**Return**
true when the cancellation request was successful. False when it is too late to cancel.

