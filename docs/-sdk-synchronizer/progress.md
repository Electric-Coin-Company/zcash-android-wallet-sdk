[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [SdkSynchronizer](index.md) / [progress](./progress.md)

# progress

`fun progress(): ReceiveChannel<`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`>`

Overrides [Synchronizer.progress](../-synchronizer/progress.md)

A stream of progress values, corresponding to this Synchronizer downloading blocks, delegated to the
[downloader](#). Any non-zero value below 100 indicates that progress indicators can be shown and a value of 100
signals that progress is complete and any progress indicators can be hidden. At that point, the synchronizer
switches from catching up on missed blocks to periodically monitoring for newly mined blocks.

