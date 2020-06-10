[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk](../index.md) / [SdkSynchronizer](index.md) / [onChainErrorHandler](./on-chain-error-handler.md)

# onChainErrorHandler

`var onChainErrorHandler: ((errorHeight: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, rewindHeight: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`) -> `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`)?`

A callback to invoke whenever a chain error is encountered. These occur whenever the
processor detects a missing or non-chain-sequential block (i.e. a reorg).

