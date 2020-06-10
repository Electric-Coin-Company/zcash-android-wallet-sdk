[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk](../index.md) / [Synchronizer](index.md) / [processorInfo](./processor-info.md)

# processorInfo

`abstract val processorInfo: Flow<ProcessorInfo>`

A flow of processor details, updated every time blocks are processed to include the latest
block height, blocks downloaded and blocks scanned. Similar to the [progress](progress.md) flow but with a
lot more detail.

