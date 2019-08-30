[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [SdkSynchronizer](./index.md)

# SdkSynchronizer

`@ExperimentalCoroutinesApi class SdkSynchronizer : `[`Synchronizer`](../-synchronizer/index.md)

A synchronizer that attempts to remain operational, despite any number of errors that can occur. It acts as the glue
that ties all the pieces of the SDK together. Each component of the SDK is designed for the potential of stand-alone
usage but coordinating all the interactions is non-trivial. So the synchronizer facilitates this, acting as reference
that demonstrates how all the pieces can be tied together. Its goal is to allow a developer to focus on their app
rather than the nuances of how Zcash works.

### Parameters

`wallet` - the component that wraps the JNI layer that interacts with the rust backend and manages wallet config.

`repository` - the component that exposes streams of wallet transaction information.

`sender` - the component responsible for sending transactions to lightwalletd in order to spend funds.

`processor` - the component that saves the downloaded compact blocks to the cache and then scans those blocks for
data related to this wallet.

`encoder` - the component that creates a signed transaction, used for spending funds.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `SdkSynchronizer(wallet: `[`Wallet`](../../cash.z.wallet.sdk.secure/-wallet/index.md)`, ledger: `[`TransactionRepository`](../-transaction-repository/index.md)`, sender: `[`TransactionSender`](../-transaction-sender/index.md)`, processor: `[`CompactBlockProcessor`](../../cash.z.wallet.sdk.block/-compact-block-processor/index.md)`, encoder: `[`TransactionEncoder`](../-transaction-encoder/index.md)`)`<br>A synchronizer that attempts to remain operational, despite any number of errors that can occur. It acts as the glue that ties all the pieces of the SDK together. Each component of the SDK is designed for the potential of stand-alone usage but coordinating all the interactions is non-trivial. So the synchronizer facilitates this, acting as reference that demonstrates how all the pieces can be tied together. Its goal is to allow a developer to focus on their app rather than the nuances of how Zcash works. |

### Properties

| Name | Summary |
|---|---|
| [coroutineScope](coroutine-scope.md) | `lateinit var coroutineScope: CoroutineScope`<br>The lifespan of this Synchronizer. This scope is initialized once the Synchronizer starts because it will be a child of the parentScope that gets passed into the [start](start.md) function. Everything launched by this Synchronizer will be cancelled once the Synchronizer or its parentScope stops. This is a lateinit rather than nullable property so that it fails early rather than silently, whenever the scope is used before the Synchronizer has been started. |
| [isConnected](is-connected.md) | `val isConnected: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>A property that is true while a connection to the lightwalletd server exists. |
| [isScanning](is-scanning.md) | `val isScanning: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>A property that is true while actively scanning the cache of compact blocks for transactions. |
| [isSyncing](is-syncing.md) | `val isSyncing: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>A property that is true while actively downloading blocks from lightwalletd. |
| [onCriticalErrorHandler](on-critical-error-handler.md) | `var onCriticalErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>A callback to invoke whenever an uncaught error is encountered. By definition, the return value of the function is ignored because this error is unrecoverable. The only reason the function has a return value is so that all error handlers work with the same signature which allows one function to handle all errors in simple apps. This callback is not called on the main thread so any UI work would need to switch context to the main thread. |
| [onProcessorErrorHandler](on-processor-error-handler.md) | `var onProcessorErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>A callback to invoke whenver a processor error is encountered. Returning true signals that the error was handled and a retry attempt should be made, if possible. This callback is not called on the main thread so any UI work would need to switch context to the main thread. |
| [onSubmissionErrorHandler](on-submission-error-handler.md) | `var onSubmissionErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>A callback to invoke whenever a server error is encountered while submitting a transaction to lightwalletd. Returning true signals that the error was handled and a retry attempt should be made, if possible. This callback is not called on the main thread so any UI work would need to switch context to the main thread. |

### Functions

| Name | Summary |
|---|---|
| [balances](balances.md) | `fun balances(): ReceiveChannel<`[`Wallet.WalletBalance`](../../cash.z.wallet.sdk.secure/-wallet/-wallet-balance/index.md)`>`<br>A stream of balance values, separately reflecting both the available and total balance. |
| [cancelSend](cancel-send.md) | `fun cancelSend(transaction: `[`SentTransaction`](../../cash.z.wallet.sdk.entity/-sent-transaction/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Attempts to cancel a previously sent transaction. Typically, cancellation is only an option if the transaction has not yet been submitted to the server. |
| [clearedTransactions](cleared-transactions.md) | `fun clearedTransactions(): ReceiveChannel<`[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`ClearedTransaction`](../../cash.z.wallet.sdk.entity/-cleared-transaction/index.md)`>>`<br>A stream of all the transactions that are on the blockchain. Implementations should consider only returning a subset like the most recent 100 transactions, perhaps through paging the underlying database. |
| [getAddress](get-address.md) | `suspend fun getAddress(accountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Gets the address for the given account. |
| [lastBalance](last-balance.md) | `fun lastBalance(): `[`Wallet.WalletBalance`](../../cash.z.wallet.sdk.secure/-wallet/-wallet-balance/index.md)<br>Holds the most recent value that was transmitted through the [balances](../-synchronizer/balances.md) channel. Typically, if the underlying channel is a BroadcastChannel (and it should be), then this value is simply [balanceChannel.value](#) |
| [lastCleared](last-cleared.md) | `fun lastCleared(): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`ClearedTransaction`](../../cash.z.wallet.sdk.entity/-cleared-transaction/index.md)`>`<br>Holds the most recent value that was transmitted through the [clearedTransactions](../-synchronizer/cleared-transactions.md) channel. Typically, if the underlying channel is a BroadcastChannel (and it should be), then this value is simply [clearedChannel.value](#) |
| [lastPending](last-pending.md) | `fun lastPending(): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`PendingTransaction`](../../cash.z.wallet.sdk.entity/-pending-transaction/index.md)`>`<br>Holds the most recent value that was transmitted through the [pendingTransactions](../-synchronizer/pending-transactions.md) channel. Typically, if the underlying channel is a BroadcastChannel (and it should be),then this value is simply [pendingChannel.value](#) |
| [onTransactionsChanged](on-transactions-changed.md) | `fun onTransactionsChanged(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [pendingTransactions](pending-transactions.md) | `fun pendingTransactions(): ReceiveChannel<`[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`PendingTransaction`](../../cash.z.wallet.sdk.entity/-pending-transaction/index.md)`>>`<br>A stream of all the outbound pending transaction that have been sent but are awaiting confirmations. |
| [progress](progress.md) | `fun progress(): ReceiveChannel<`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`>`<br>A stream of progress values, typically corresponding to this Synchronizer downloading blocks. Typically, any non- zero value below 100 indicates that progress indicators can be shown and a value of 100 signals that progress is complete and any progress indicators can be hidden. |
| [refreshBalance](refresh-balance.md) | `suspend fun refreshBalance(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [sendToAddress](send-to-address.md) | `suspend fun sendToAddress(zatoshi: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fromAccountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`PendingTransaction`](../../cash.z.wallet.sdk.entity/-pending-transaction/index.md)<br>Sends zatoshi. |
| [start](start.md) | `fun start(parentScope: CoroutineScope): `[`Synchronizer`](../-synchronizer/index.md)<br>Starts this synchronizer within the given scope. For simplicity, attempting to start an instance that has already been started will throw a [SynchronizerException.FalseStart](../../cash.z.wallet.sdk.exception/-synchronizer-exception/-false-start.md) exception. This reduces the complexity of managing resources that must be recycled. Instead, each synchronizer is designed to have a long lifespan and should be started from an activity, application or session. |
| [stop](stop.md) | `fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Stop this synchronizer and all of its child jobs. Once a synchronizer has been stopped it should not be restarted and attempting to do so will result in an error. Also, this function will throw an exception if the synchronizer was never previously started. |
