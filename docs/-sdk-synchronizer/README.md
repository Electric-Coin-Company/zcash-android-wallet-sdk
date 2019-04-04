[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [SdkSynchronizer](./index.md)

# SdkSynchronizer

`class SdkSynchronizer : `[`Synchronizer`](../-synchronizer/index.md)

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

### Types

| Name | Summary |
|---|---|
| [SyncState](-sync-state/index.md) | `sealed class SyncState`<br>Represents the initial state of the Synchronizer. |

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `SdkSynchronizer(downloader: `[`CompactBlockStream`](../-compact-block-stream/index.md)`, processor: `[`CompactBlockProcessor`](../-compact-block-processor/index.md)`, repository: `[`TransactionRepository`](../-transaction-repository/index.md)`, activeTransactionManager: `[`ActiveTransactionManager`](../-active-transaction-manager/index.md)`, wallet: `[`Wallet`](../../cash.z.wallet.sdk.secure/-wallet/index.md)`, batchSize: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 1000, staleTolerance: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 10, blockPollFrequency: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)` = CompactBlockStream.DEFAULT_POLL_INTERVAL)`<br>The glue. Downloads compact blocks to the database and then scans them for transactions. In order to serve that purpose, this class glues together a variety of key components. Each component contributes to the team effort of providing a simple source of truth to interact with. |

### Properties

| Name | Summary |
|---|---|
| [onSynchronizerErrorListener](on-synchronizer-error-listener.md) | `var onSynchronizerErrorListener: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>Sets a listener to be notified of uncaught Synchronizer errors. When null, errors will only be logged. |

### Functions

| Name | Summary |
|---|---|
| [activeTransactions](active-transactions.md) | `fun activeTransactions(): ReceiveChannel<`[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`ActiveTransaction`](../-active-transaction/index.md)`, `[`TransactionState`](../-transaction-state/index.md)`>>`<br>A stream of all the wallet transactions, delegated to the [activeTransactionManager](#). |
| [allTransactions](all-transactions.md) | `fun allTransactions(): ReceiveChannel<`[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`WalletTransaction`](../../cash.z.wallet.sdk.dao/-wallet-transaction/index.md)`>>`<br>A stream of all the wallet transactions, delegated to the [repository](#). |
| [balance](balance.md) | `fun balance(): ReceiveChannel<`[`Wallet.WalletBalance`](../../cash.z.wallet.sdk.secure/-wallet/-wallet-balance/index.md)`>`<br>A stream of balance values, delegated to the [wallet](#). |
| [cancelSend](cancel-send.md) | `fun cancelSend(transaction: `[`ActiveSendTransaction`](../-active-send-transaction/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Attempts to cancel a previously sent transaction. Transactions can only be cancelled during the calculation phase before they've been submitted to the server. This method will return false when it is too late to cancel. This logic is delegated to the activeTransactionManager, which knows the state of the given transaction. |
| [getAddress](get-address.md) | `fun getAddress(accountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Gets the address for the given account. |
| [isFirstRun](is-first-run.md) | `suspend fun isFirstRun(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>A flag to indicate that the initial state of this synchronizer was firstRun. This is useful for knowing whether initializing the database is required and whether to show things like"first run walk-throughs." |
| [isStale](is-stale.md) | `suspend fun isStale(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>A flag to indicate that this Synchronizer is significantly out of sync with it's server. This is determined by the delta between the current block height reported by the server and the latest block we have stored in cache. Whenever this delta is greater than the [staleTolerance](#), this function returns true. This is intended for showing progress indicators when the user returns to the app after having not used it for a long period. Typically, this means the user may have to wait for downloading to occur and the current balance and transaction information cannot be trusted as 100% accurate. |
| [progress](progress.md) | `fun progress(): ReceiveChannel<`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`>`<br>A stream of progress values, corresponding to this Synchronizer downloading blocks, delegated to the [downloader](#). Any non-zero value below 100 indicates that progress indicators can be shown and a value of 100 signals that progress is complete and any progress indicators can be hidden. At that point, the synchronizer switches from catching up on missed blocks to periodically monitoring for newly mined blocks. |
| [sendToAddress](send-to-address.md) | `suspend fun sendToAddress(zatoshi: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fromAccountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Sends zatoshi. |
| [start](start.md) | `fun start(parentScope: CoroutineScope): `[`Synchronizer`](../-synchronizer/index.md)<br>Starts this synchronizer within the given scope. For simplicity, attempting to start an instance that has already been started will throw a [SynchronizerException.FalseStart](../../cash.z.wallet.sdk.exception/-synchronizer-exception/-false-start.md) exception. This reduces the complexity of managing resources that must be recycled. Instead, each synchronizer is designed to have a long lifespan and should be started from an activity, application or session. |
| [stop](stop.md) | `fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Stops this synchronizer by stopping the downloader, repository, and activeTransactionManager, then cancelling the parent job. Note that we do not cancel the parent scope that was passed into [start](start.md) because the synchronizer does not own that scope, it just uses it for launching children. |
