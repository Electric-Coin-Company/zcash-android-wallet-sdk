[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk](../index.md) / [Synchronizer](index.md) / [cancelSpend](./cancel-spend.md)

# cancelSpend

`abstract suspend fun cancelSpend(transaction: `[`PendingTransaction`](../../cash.z.ecc.android.sdk.db.entity/-pending-transaction/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Attempts to cancel a transaction that is about to be sent. Typically, cancellation is only
an option if the transaction has not yet been submitted to the server.

### Parameters

`transaction` - the transaction to cancel.

**Return**
true when the cancellation request was successful. False when it is too late.

