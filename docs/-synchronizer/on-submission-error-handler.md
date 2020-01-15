[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk](../index.md) / [Synchronizer](index.md) / [onSubmissionErrorHandler](./on-submission-error-handler.md)

# onSubmissionErrorHandler

`abstract var onSubmissionErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`

An error handler for exceptions while submitting transactions to lightwalletd. For instance,
a transaction may get rejected because it would be a double-spend or the user might lose
their cellphone signal.

**Return**
true when the error has been handled and the sender should attempt to resend. False
when the error is unrecoverable and the sender should [stop](stop.md).

