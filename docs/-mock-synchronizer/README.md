[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [MockSynchronizer](./index.md)

# MockSynchronizer

`open class MockSynchronizer : `[`Synchronizer`](../-synchronizer/index.md)`, CoroutineScope`

Utility for building UIs. It does the best it can to mock the SDKSynchronizer so that it can be dropped into any
project and drive the UI. It generates active transactions in response to funds being sent and generates random
received transactions, periodically.

### Parameters

`transactionInterval` - the time in milliseconds between receive transactions being added because those are the
only ones auto-generated. Send transactions are triggered by the UI. Transactions are polled at half this interval.

`initialLoadDuration` - the time in milliseconds it should take to simulate the initial load. The progress channel
will send regular updates such that it reaches 100 in this amount of time.

`activeTransactionUpdateFrequency` - the amount of time in milliseconds between updates to an active
transaction's state. Active transactions move through their lifecycle and increment their state at this rate.

`isFirstRun` - whether this Mock should return `true` for isFirstRun. Defaults to a random boolean.

`isStale` - whether this Mock should return `true` for isStale. When null, this will follow the default behavior
of returning true about 10% of the time.

`onSynchronizerErrorListener` - presently ignored because there are not yet any errors in mock.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `MockSynchronizer(transactionInterval: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)` = 30_000L, initialLoadDuration: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)` = 5_000L, activeTransactionUpdateFrequency: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)` = 3_000L, isFirstRun: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = Random.nextBoolean(), isStale: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`? = null, onSynchronizerErrorListener: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)? = null)`<br>Utility for building UIs. It does the best it can to mock the SDKSynchronizer so that it can be dropped into any project and drive the UI. It generates active transactions in response to funds being sent and generates random received transactions, periodically. |

### Properties

| Name | Summary |
|---|---|
| [coroutineContext](coroutine-context.md) | `open val coroutineContext: `[`CoroutineContext`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html)<br>Coroutine context used for the CoroutineScope implementation, used to mock asynchronous behaviors. |
| [onSynchronizerErrorListener](on-synchronizer-error-listener.md) | `open var onSynchronizerErrorListener: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>presently ignored because there are not yet any errors in mock. |

### Functions

| Name | Summary |
|---|---|
| [activeTransactions](active-transactions.md) | `open fun activeTransactions(): ReceiveChannel<`[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`ActiveTransaction`](../-active-transaction/index.md)`, `[`TransactionState`](../-transaction-state/index.md)`>>`<br>A stream of all the active transactions. |
| [allTransactions](all-transactions.md) | `open fun allTransactions(): ReceiveChannel<`[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`WalletTransaction`](../../cash.z.wallet.sdk.dao/-wallet-transaction/index.md)`>>`<br>A stream of all the wallet transactions. |
| [balance](balance.md) | `open fun balance(): ReceiveChannel<`[`Wallet.WalletBalance`](../../cash.z.wallet.sdk.secure/-wallet/-wallet-balance/index.md)`>`<br>A stream of balance values. |
| [cancelSend](cancel-send.md) | `open fun cancelSend(transaction: `[`ActiveSendTransaction`](../-active-send-transaction/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Sets the state of the given transaction to 'Cancelled'. |
| [getAddress](get-address.md) | `open fun getAddress(accountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Returns the [mockAddress](#). This address is not usable. |
| [isFirstRun](is-first-run.md) | `open suspend fun isFirstRun(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns [isFirstRun](is-first-run.md) as provided during initialization of this MockSynchronizer. |
| [isStale](is-stale.md) | `open suspend fun isStale(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true roughly 10% of the time and then resets to false after some delay. |
| [progress](progress.md) | `open fun progress(): ReceiveChannel<`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`>`<br>A stream of progress values, typically corresponding to this Synchronizer downloading blocks. Typically, any non- zero value below 100 indicates that progress indicators can be shown and a value of 100 signals that progress is complete and any progress indicators can be hidden. |
| [sendToAddress](send-to-address.md) | `open suspend fun sendToAddress(zatoshi: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fromAccountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Uses the [forge](#) to fabricate a transaction and then walk it through the transaction lifecycle in a useful way. This method will validate the zatoshi amount and toAddress a bit to help with UI validation. |
| [start](start.md) | `open fun start(parentScope: CoroutineScope): `[`Synchronizer`](../-synchronizer/index.md)<br>Starts this mock Synchronizer. |
| [stop](stop.md) | `open fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Stops this mock Synchronizer by cancelling its primary job. |
