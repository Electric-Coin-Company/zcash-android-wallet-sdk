[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk](../index.md) / [SdkSynchronizer](index.md) / [latestHeight](./latest-height.md)

# latestHeight

`val latestHeight: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)

Convenience function for the latest height. Specifically, this value represents the last
height that the synchronizer has observed from the lightwalletd server. Instead of using
this, a wallet will more likely want to consume the flow of processor info using
[processorInfo](processor-info.md).

