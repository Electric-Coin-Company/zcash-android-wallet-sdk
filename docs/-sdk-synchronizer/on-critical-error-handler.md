[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk](../index.md) / [SdkSynchronizer](index.md) / [onCriticalErrorHandler](./on-critical-error-handler.md)

# onCriticalErrorHandler

`var onCriticalErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`

Overrides [Synchronizer.onCriticalErrorHandler](../-synchronizer/on-critical-error-handler.md)

A callback to invoke whenever an uncaught error is encountered. By definition, the return
value of the function is ignored because this error is unrecoverable. The only reason the
function has a return value is so that all error handlers work with the same signature which
allows one function to handle all errors in simple apps. This callback is not called on the
main thread so any UI work would need to switch context to the main thread.

