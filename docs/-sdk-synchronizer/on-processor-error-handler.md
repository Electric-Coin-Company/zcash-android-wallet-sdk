[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk](../index.md) / [SdkSynchronizer](index.md) / [onProcessorErrorHandler](./on-processor-error-handler.md)

# onProcessorErrorHandler

`var onProcessorErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`

Overrides [Synchronizer.onProcessorErrorHandler](../-synchronizer/on-processor-error-handler.md)

A callback to invoke whenever a processor error is encountered. Returning true signals that
the error was handled and a retry attempt should be made, if possible. This callback is not
called on the main thread so any UI work would need to switch context to the main thread.

