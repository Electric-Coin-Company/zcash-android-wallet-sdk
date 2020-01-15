[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk](../index.md) / [Synchronizer](index.md) / [processorInfo](./processor-info.md)

# processorInfo

`abstract val processorInfo: Flow<`[`CompactBlockProcessor.ProcessorInfo`](../../cash.z.wallet.sdk.block/-compact-block-processor/-processor-info/index.md)`>`

A flow of processor details, updated every time blocks are processed to include the latest
block height, blocks downloaded and blocks scanned. Similar to the [progress](progress.md) flow but with a
lot more detail.

