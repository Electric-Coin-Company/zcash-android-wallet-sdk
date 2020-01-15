[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk](../index.md) / [Synchronizer](index.md) / [onProcessorErrorHandler](./on-processor-error-handler.md)

# onProcessorErrorHandler

`abstract var onProcessorErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`

An error handler for exceptions during processing. For instance, a block might be missing or
a reorg may get mishandled or the database may get corrupted.

**Return**
true when the error has been handled and the processor should attempt to continue.
False when the error is unrecoverable and the processor should [stop](stop.md).

