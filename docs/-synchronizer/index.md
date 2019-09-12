[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [Synchronizer](./index.md)

# Synchronizer

`interface Synchronizer`

Primary interface for interacting with the SDK. Defines the contract that specific implementations like
[MockSynchronizer](../-mock-synchronizer/index.md) and [SdkSynchronizer](../-sdk-synchronizer/index.md) fulfill. Given the language-level support for coroutines, we favor their
use in the SDK and incorporate that choice into this contract.

### Properties

| Name | Summary |
|---|---|
| [isConnected](is-connected.md) | `abstract val isConnected: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>A flag indicating whether this Synchronizer is connected to its lightwalletd server. When false, a UI element may want to turn red. |
| [isScanning](is-scanning.md) | `abstract val isScanning: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>A flag indicating whether this Synchronizer is actively decrypting compact blocks, searching for transactions. When true, a UI element may want to turn yellow. |
| [isSyncing](is-syncing.md) | `abstract val isSyncing: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>A flag indicating whether this Synchronizer is actively downloading compact blocks. When true, a UI element may want to turn yellow. |
| [onCriticalErrorHandler](on-critical-error-handler.md) | `abstract var onCriticalErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>Gets or sets a global error handler. This is a useful hook for handling unexpected critical errors. |
| [onProcessorErrorHandler](on-processor-error-handler.md) | `abstract var onProcessorErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>An error handler for exceptions during processing. For instance, a block might be missing or a reorg may get mishandled or the database may get corrupted. |
| [onSubmissionErrorHandler](on-submission-error-handler.md) | `abstract var onSubmissionErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>An error handler for exceptions while submitting transactions to lightwalletd. For instance, a transaction may get rejected because it would be a double-spend or the user might lose their cellphone signal. |

### Functions

| Name | Summary |
|---|---|
| [balances](balances.md) | `abstract fun balances(): ReceiveChannel<`[`Wallet.WalletBalance`](../../cash.z.wallet.sdk.secure/-wallet/-wallet-balance/index.md)`>`<br>A stream of balance values, separately reflecting both the available and total balance. |
| [cancelSend](cancel-send.md) | `abstract fun cancelSend(transaction: `[`SentTransaction`](../../cash.z.wallet.sdk.entity/-sent-transaction/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Attempts to cancel a previously sent transaction. Typically, cancellation is only an option if the transaction has not yet been submitted to the server. |
| [clearedTransactions](cleared-transactions.md) | `abstract fun clearedTransactions(): ReceiveChannel<`[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`ClearedTransaction`](../../cash.z.wallet.sdk.entity/-cleared-transaction/index.md)`>>`<br>A stream of all the transactions that are on the blockchain. Implementations should consider only returning a subset like the most recent 100 transactions, perhaps through paging the underlying database. |
| [getAddress](get-address.md) | `abstract suspend fun getAddress(accountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 0): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Gets the address for the given account. |
| [lastBalance](last-balance.md) | `abstract fun lastBalance(): `[`Wallet.WalletBalance`](../../cash.z.wallet.sdk.secure/-wallet/-wallet-balance/index.md)<br>Holds the most recent value that was transmitted through the [balances](balances.md) channel. Typically, if the underlying channel is a BroadcastChannel (and it should be), then this value is simply [balanceChannel.value](#) |
| [lastCleared](last-cleared.md) | `abstract fun lastCleared(): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`ClearedTransaction`](../../cash.z.wallet.sdk.entity/-cleared-transaction/index.md)`>`<br>Holds the most recent value that was transmitted through the [clearedTransactions](cleared-transactions.md) channel. Typically, if the underlying channel is a BroadcastChannel (and it should be), then this value is simply [clearedChannel.value](#) |
| [lastPending](last-pending.md) | `abstract fun lastPending(): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`PendingTransaction`](../../cash.z.wallet.sdk.entity/-pending-transaction/index.md)`>`<br>Holds the most recent value that was transmitted through the [pendingTransactions](pending-transactions.md) channel. Typically, if the underlying channel is a BroadcastChannel (and it should be),then this value is simply [pendingChannel.value](#) |
| [pendingTransactions](pending-transactions.md) | `abstract fun pendingTransactions(): ReceiveChannel<`[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`PendingTransaction`](../../cash.z.wallet.sdk.entity/-pending-transaction/index.md)`>>`<br>A stream of all the outbound pending transaction that have been sent but are awaiting confirmations. |
| [progress](progress.md) | `abstract fun progress(): ReceiveChannel<`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`>`<br>A stream of progress values, typically corresponding to this Synchronizer downloading blocks. Typically, any non- zero value below 100 indicates that progress indicators can be shown and a value of 100 signals that progress is complete and any progress indicators can be hidden. |
| [sendToAddress](send-to-address.md) | `abstract suspend fun sendToAddress(zatoshi: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = "", fromAccountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 0): `[`PendingTransaction`](../../cash.z.wallet.sdk.entity/-pending-transaction/index.md)<br>Sends zatoshi. |
| [start](start.md) | `abstract fun start(parentScope: CoroutineScope): `[`Synchronizer`](./index.md)<br>Starts this synchronizer within the given scope. |
| [stop](stop.md) | `abstract fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Stop this synchronizer. Implementations should ensure that calling this method cancels all jobs that were created by this instance. |

### Inheritors

| Name | Summary |
|---|---|
| [MockSynchronizer](../-mock-synchronizer/index.md) | `abstract class MockSynchronizer : `[`Synchronizer`](./index.md)`, CoroutineScope`<br>Utility for building UIs. It does the best it can to mock the SDKSynchronizer so that it can be dropped into any project and drive the UI. It generates active transactions in response to funds being sent and generates random received transactions, periodically. |
| [SdkSynchronizer](../-sdk-synchronizer/index.md) | `class SdkSynchronizer : `[`Synchronizer`](./index.md)<br>A synchronizer that attempts to remain operational, despite any number of errors that can occur. It acts as the glue that ties all the pieces of the SDK together. Each component of the SDK is designed for the potential of stand-alone usage but coordinating all the interactions is non-trivial. So the synchronizer facilitates this, acting as reference that demonstrates how all the pieces can be tied together. Its goal is to allow a developer to focus on their app rather than the nuances of how Zcash works. |
