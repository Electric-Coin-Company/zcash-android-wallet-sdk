[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [SdkSynchronizer](index.md) / [cancelSend](./cancel-send.md)

# cancelSend

`fun cancelSend(transaction: `[`ActiveSendTransaction`](../-active-send-transaction/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Overrides [Synchronizer.cancelSend](../-synchronizer/cancel-send.md)

Attempts to cancel a previously sent transaction. Transactions can only be cancelled during the calculation phase
before they've been submitted to the server. This method will return false when it is too late to cancel. This
logic is delegated to the activeTransactionManager, which knows the state of the given transaction.

### Parameters

`transaction` - the transaction to cancel.

**Return**
true when the cancellation request was successful. False when it is too late to cancel.

