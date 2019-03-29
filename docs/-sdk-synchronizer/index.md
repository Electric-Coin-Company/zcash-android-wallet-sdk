[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [SdkSynchronizer](./index.md)

# SdkSynchronizer

`class SdkSynchronizer : `[`Synchronizer`](../-synchronizer/index.md)

The glue. Downloads compact blocks to the database and then scans them for transactions. In order to serve that
purpose, this class glues together a variety of key components. Each component contributes to the team effort of
providing a simple source of truth to interact with.

Another way of thinking about this class is the reference that demonstrates how all the pieces can be tied
together.

### Types

| Name | Summary |
|---|---|
| [SyncState](-sync-state/index.md) | `sealed class SyncState` |

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `SdkSynchronizer(downloader: `[`CompactBlockStream`](../-compact-block-stream/index.md)`, processor: `[`CompactBlockProcessor`](../-compact-block-processor/index.md)`, repository: `[`TransactionRepository`](../-transaction-repository/index.md)`, activeTransactionManager: `[`ActiveTransactionManager`](../-active-transaction-manager/index.md)`, wallet: `[`Wallet`](../../cash.z.wallet.sdk.secure/-wallet/index.md)`, batchSize: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 1000, blockPollFrequency: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)` = CompactBlockStream.DEFAULT_POLL_INTERVAL)`<br>The glue. Downloads compact blocks to the database and then scans them for transactions. In order to serve that purpose, this class glues together a variety of key components. Each component contributes to the team effort of providing a simple source of truth to interact with. |

### Properties

| Name | Summary |
|---|---|
| [onSynchronizerErrorListener](on-synchronizer-error-listener.md) | `var onSynchronizerErrorListener: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>Gets or sets a global error listener. This is a useful hook for handling unexpected critical errors. |

### Functions

| Name | Summary |
|---|---|
| [activeTransactions](active-transactions.md) | `fun activeTransactions(): ReceiveChannel<`[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`ActiveTransaction`](../-active-transaction/index.md)`, `[`TransactionState`](../-transaction-state/index.md)`>>`<br>A stream of all the active transactions. |
| [allTransactions](all-transactions.md) | `fun allTransactions(): ReceiveChannel<`[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`WalletTransaction`](../../cash.z.wallet.sdk.dao/-wallet-transaction/index.md)`>>`<br>A stream of all the wallet transactions. |
| [balance](balance.md) | `fun balance(): ReceiveChannel<`[`Wallet.WalletBalance`](../../cash.z.wallet.sdk.secure/-wallet/-wallet-balance/index.md)`>`<br>A stream of balance values. |
| [cancelSend](cancel-send.md) | `fun cancelSend(transaction: `[`ActiveSendTransaction`](../-active-send-transaction/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Attempts to cancel a previously sent transaction. Typically, cancellation is only an option if the transaction has not yet been submitted to the server. |
| [getAddress](get-address.md) | `fun getAddress(accountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Gets the address for the given account. |
| [isFirstRun](is-first-run.md) | `suspend fun isFirstRun(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>A flag to indicate that this is the first run of this Synchronizer on this device. This is useful for knowing whether to initialize databases or other required resourcews, as well as whether to show walk-throughs. |
| [isStale](is-stale.md) | `suspend fun isStale(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>A flag to indicate that this Synchronizer is significantly out of sync with it's server. Typically, this means that the balance and other data cannot be completely trusted because a significant amount of data has not been processed. This is intended for showing progress indicators when the user returns to the app after having not used it for days. Typically, this means minor sync issues should be ignored and this should be leveraged in order to alert a user that the balance information is stale. |
| [progress](progress.md) | `fun progress(): ReceiveChannel<`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`>`<br>A stream of progress values, typically corresponding to this Synchronizer downloading blocks. Typically, any non- zero value below 100 indicates that progress indicators can be shown and a value of 100 signals that progress is complete and any progress indicators can be hidden. |
| [sendToAddress](send-to-address.md) | `suspend fun sendToAddress(zatoshi: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fromAccountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Sends zatoshi. |
| [start](start.md) | `fun start(parentScope: CoroutineScope): `[`Synchronizer`](../-synchronizer/index.md)<br>Starts this synchronizer within the given scope. For simplicity, attempting to start an instance that has already been started will throw a [SynchronizerException.FalseStart](../../cash.z.wallet.sdk.exception/-synchronizer-exception/-false-start.md) exception. This reduces the complexity of managing resources that must be recycled. Instead, each synchronizer is designed to have a long lifespan (similar to act or application) &lt;=- explain usage |
| [stop](stop.md) | `fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Stop this synchronizer. |
