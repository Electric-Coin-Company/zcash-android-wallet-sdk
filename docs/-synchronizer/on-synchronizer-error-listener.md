[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk.data](../index.md) / [Synchronizer](index.md) / [onSynchronizerErrorListener](./on-synchronizer-error-listener.md)

# onSynchronizerErrorListener

`abstract var onSynchronizerErrorListener: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`

Gets or sets a global error listener. This is a useful hook for handling unexpected critical errors.

**Return**
true when the error has been handled and the Synchronizer should continue. False when the error is
unrecoverable and the Synchronizer should [stop](stop.md).

