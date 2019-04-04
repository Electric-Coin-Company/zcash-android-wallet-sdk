[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [SdkSynchronizer](index.md) / [&lt;init&gt;](./-init-.md)

# &lt;init&gt;

`SdkSynchronizer(downloader: `[`CompactBlockStream`](../-compact-block-stream/index.md)`, processor: `[`CompactBlockProcessor`](../-compact-block-processor/index.md)`, repository: `[`TransactionRepository`](../-transaction-repository/index.md)`, activeTransactionManager: `[`ActiveTransactionManager`](../-active-transaction-manager/index.md)`, wallet: `[`Wallet`](../../cash.z.wallet.sdk.secure/-wallet/index.md)`, batchSize: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 1000, staleTolerance: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 10, blockPollFrequency: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)` = CompactBlockStream.DEFAULT_POLL_INTERVAL)`

The glue. Downloads compact blocks to the database and then scans them for transactions. In order to serve that
purpose, this class glues together a variety of key components. Each component contributes to the team effort of
providing a simple source of truth to interact with.

Another way of thinking about this class is the reference that demonstrates how all the pieces can be tied
together.

### Parameters

`downloader` - the component that downloads compact blocks and exposes them as a stream

`processor` - the component that saves the downloaded compact blocks to the cache and then scans those blocks for
data related to this wallet.

`repository` - the component that exposes streams of wallet transaction information.

`activeTransactionManager` - the component that manages the lifecycle of active transactions. This includes sent
transactions that have not been mined.

`wallet` - the component that wraps the JNI layer that interacts with librustzcash and manages wallet config.

`batchSize` - the number of compact blocks to download at a time.

`staleTolerance` - the number of blocks to allow before considering our data to be stale

`blockPollFrequency` - how often to poll for compact blocks. Once all missing blocks have been downloaded, this
number represents the number of milliseconds the synchronizer will wait before checking for newly mined blocks.