[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk](../index.md) / [SdkSynchronizer](index.md) / [onSubmissionErrorHandler](./on-submission-error-handler.md)

# onSubmissionErrorHandler

`var onSubmissionErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`

A callback to invoke whenever a server error is encountered while submitting a transaction to
lightwalletd. Returning true signals that the error was handled and a retry attempt should be
made, if possible. This callback is not called on the main thread so any UI work would need
to switch context to the main thread.

