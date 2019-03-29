[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [SdkSynchronizer](index.md) / [&lt;init&gt;](./-init-.md)

# &lt;init&gt;

`SdkSynchronizer(downloader: `[`CompactBlockStream`](../-compact-block-stream/index.md)`, processor: `[`CompactBlockProcessor`](../-compact-block-processor/index.md)`, repository: `[`TransactionRepository`](../-transaction-repository/index.md)`, activeTransactionManager: `[`ActiveTransactionManager`](../-active-transaction-manager/index.md)`, wallet: `[`Wallet`](../../cash.z.wallet.sdk.secure/-wallet/index.md)`, batchSize: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 1000, blockPollFrequency: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)` = CompactBlockStream.DEFAULT_POLL_INTERVAL)`

The glue. Downloads compact blocks to the database and then scans them for transactions. In order to serve that
purpose, this class glues together a variety of key components. Each component contributes to the team effort of
providing a simple source of truth to interact with.

Another way of thinking about this class is the reference that demonstrates how all the pieces can be tied
together.

