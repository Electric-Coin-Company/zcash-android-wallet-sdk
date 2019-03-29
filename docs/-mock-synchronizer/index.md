[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [MockSynchronizer](./index.md)

# MockSynchronizer

`open class MockSynchronizer : `[`Synchronizer`](../-synchronizer/index.md)`, CoroutineScope`

Utility for building UIs. It does the best it can to mock the synchronizer so that it can be dropped right into any
project and drive the UI. It generates active transactions in response to funds being sent and generates random
received transactions periodically.

### Parameters

`transactionInterval` - the time in milliseconds between receive transactions being added because those are the
only ones auto-generated. Send transactions are triggered by the UI. Transactions are polled at half this interval.

`activeTransactionUpdateFrequency` - the amount of time in milliseconds between updates to an active
transaction's state. Active transactions move through their lifecycle and increment their state at this rate.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `MockSynchronizer(transactionInterval: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)` = 30_000L, initialLoadDuration: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)` = 5_000L, activeTransactionUpdateFrequency: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)` = 3_000L, isFirstRun: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = Random.nextBoolean(), isOutOfSync: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`? = null, onSynchronizerErrorListener: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)? = null)`<br>Utility for building UIs. It does the best it can to mock the synchronizer so that it can be dropped right into any project and drive the UI. It generates active transactions in response to funds being sent and generates random received transactions periodically. |

### Properties

| Name | Summary |
|---|---|
| [coroutineContext](coroutine-context.md) | `open val coroutineContext: `[`CoroutineContext`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html) |
| [onSynchronizerErrorListener](on-synchronizer-error-listener.md) | `open var onSynchronizerErrorListener: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>Gets or sets a global error listener. This is a useful hook for handling unexpected critical errors. |

### Functions

| Name | Summary |
|---|---|
| [activeTransactions](active-transactions.md) | `open fun activeTransactions(): ReceiveChannel<`[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`ActiveTransaction`](../-active-transaction/index.md)`, `[`TransactionState`](../-transaction-state/index.md)`>>`<br>A stream of all the active transactions. |
| [allTransactions](all-transactions.md) | `open fun allTransactions(): ReceiveChannel<`[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`WalletTransaction`](../../cash.z.wallet.sdk.dao/-wallet-transaction/index.md)`>>`<br>A stream of all the wallet transactions. |
| [balance](balance.md) | `open fun balance(): ReceiveChannel<`[`Wallet.WalletBalance`](../../cash.z.wallet.sdk.secure/-wallet/-wallet-balance/index.md)`>`<br>A stream of balance values. |
| [cancelSend](cancel-send.md) | `open fun cancelSend(transaction: `[`ActiveSendTransaction`](../-active-send-transaction/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Attempts to cancel a previously sent transaction. Typically, cancellation is only an option if the transaction has not yet been submitted to the server. |
| [getAddress](get-address.md) | `open fun getAddress(accountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Gets the address for the given account. |
| [isFirstRun](is-first-run.md) | `open suspend fun isFirstRun(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>A flag to indicate that this is the first run of this Synchronizer on this device. This is useful for knowing whether to initialize databases or other required resourcews, as well as whether to show walk-throughs. |
| [isStale](is-stale.md) | `open suspend fun isStale(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>A flag to indicate that this Synchronizer is significantly out of sync with it's server. Typically, this means that the balance and other data cannot be completely trusted because a significant amount of data has not been processed. This is intended for showing progress indicators when the user returns to the app after having not used it for days. Typically, this means minor sync issues should be ignored and this should be leveraged in order to alert a user that the balance information is stale. |
| [progress](progress.md) | `open fun progress(): ReceiveChannel<`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`>`<br>A stream of progress values, typically corresponding to this Synchronizer downloading blocks. Typically, any non- zero value below 100 indicates that progress indicators can be shown and a value of 100 signals that progress is complete and any progress indicators can be hidden. |
| [sendToAddress](send-to-address.md) | `open suspend fun sendToAddress(zatoshi: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fromAccountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Sends zatoshi. |
| [start](start.md) | `open fun start(parentScope: CoroutineScope): `[`Synchronizer`](../-synchronizer/index.md)<br>Starts this synchronizer within the given scope. |
| [stop](stop.md) | `open fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Stop this synchronizer. |
