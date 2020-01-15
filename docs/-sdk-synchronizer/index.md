[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk](../index.md) / [SdkSynchronizer](./index.md)

# SdkSynchronizer

`@ExperimentalCoroutinesApi class SdkSynchronizer : `[`Synchronizer`](../-synchronizer/index.md)

A Synchronizer that attempts to remain operational, despite any number of errors that can occur.
It acts as the glue that ties all the pieces of the SDK together. Each component of the SDK is
designed for the potential of stand-alone usage but coordinating all the interactions is non-
trivial. So the Synchronizer facilitates this, acting as reference that demonstrates how all the
pieces can be tied together. Its goal is to allow a developer to focus on their app rather than
the nuances of how Zcash works.

### Parameters

`ledger` - exposes flows of wallet transaction information.

`manager` - manages and tracks outbound transactions.

`processor` - saves the downloaded compact blocks to the cache and then scans those blocks for
data related to this wallet.

### Properties

| Name | Summary |
|---|---|
| [balances](balances.md) | `val balances: Flow<`[`CompactBlockProcessor.WalletBalance`](../../cash.z.wallet.sdk.block/-compact-block-processor/-wallet-balance/index.md)`>`<br>A stream of balance values, separately reflecting both the available and total balance. |
| [clearedTransactions](cleared-transactions.md) | `val clearedTransactions: Flow<PagedList<`[`ConfirmedTransaction`](../../cash.z.wallet.sdk.entity/-confirmed-transaction/index.md)`>>`<br>A flow of all the transactions that are on the blockchain. |
| [coroutineScope](coroutine-scope.md) | `lateinit var coroutineScope: CoroutineScope`<br>The lifespan of this Synchronizer. This scope is initialized once the Synchronizer starts because it will be a child of the parentScope that gets passed into the [start](start.md) function. Everything launched by this Synchronizer will be cancelled once the Synchronizer or its parentScope stops. This is a lateinit rather than nullable property so that it fails early rather than silently, whenever the scope is used before the Synchronizer has been started. |
| [onCriticalErrorHandler](on-critical-error-handler.md) | `var onCriticalErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>A callback to invoke whenever an uncaught error is encountered. By definition, the return value of the function is ignored because this error is unrecoverable. The only reason the function has a return value is so that all error handlers work with the same signature which allows one function to handle all errors in simple apps. This callback is not called on the main thread so any UI work would need to switch context to the main thread. |
| [onProcessorErrorHandler](on-processor-error-handler.md) | `var onProcessorErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>A callback to invoke whenever a processor error is encountered. Returning true signals that the error was handled and a retry attempt should be made, if possible. This callback is not called on the main thread so any UI work would need to switch context to the main thread. |
| [onSubmissionErrorHandler](on-submission-error-handler.md) | `var onSubmissionErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>A callback to invoke whenever a server error is encountered while submitting a transaction to lightwalletd. Returning true signals that the error was handled and a retry attempt should be made, if possible. This callback is not called on the main thread so any UI work would need to switch context to the main thread. |
| [pendingTransactions](pending-transactions.md) | `val pendingTransactions: Flow<`[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`PendingTransaction`](../../cash.z.wallet.sdk.entity/-pending-transaction/index.md)`>>`<br>A flow of all the outbound pending transaction that have been sent but are awaiting confirmations. |
| [processor](processor.md) | `val processor: `[`CompactBlockProcessor`](../../cash.z.wallet.sdk.block/-compact-block-processor/index.md)<br>saves the downloaded compact blocks to the cache and then scans those blocks for data related to this wallet. |
| [processorInfo](processor-info.md) | `val processorInfo: Flow<`[`CompactBlockProcessor.ProcessorInfo`](../../cash.z.wallet.sdk.block/-compact-block-processor/-processor-info/index.md)`>`<br>Indicates the latest information about the blocks that have been processed by the SDK. This is very helpful for conveying detailed progress and status to the user. |
| [progress](progress.md) | `val progress: Flow<`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`>`<br>Indicates the download progress of the Synchronizer. When progress reaches 100, that signals that the Synchronizer is in sync with the network. Balances should be considered inaccurate and outbound transactions should be prevented until this sync is complete. It is a simplified version of [processorInfo](processor-info.md). |
| [receivedTransactions](received-transactions.md) | `val receivedTransactions: Flow<PagedList<`[`ConfirmedTransaction`](../../cash.z.wallet.sdk.entity/-confirmed-transaction/index.md)`>>`<br>A flow of all transactions related to receiving funds. |
| [sentTransactions](sent-transactions.md) | `val sentTransactions: Flow<PagedList<`[`ConfirmedTransaction`](../../cash.z.wallet.sdk.entity/-confirmed-transaction/index.md)`>>`<br>A flow of all transactions related to sending funds. |
| [status](status.md) | `val status: Flow<`[`Synchronizer.Status`](../-synchronizer/-status/index.md)`>`<br>Indicates the status of this Synchronizer. This implementation basically simplifies the status of the processor to focus only on the high level states that matter most. Whenever the processor is finished scanning, the synchronizer updates transaction and balance info and then emits a [SYNCED](../-synchronizer/-status/-s-y-n-c-e-d.md) status. |

### Functions

| Name | Summary |
|---|---|
| [cancelSpend](cancel-spend.md) | `suspend fun cancelSpend(transaction: `[`PendingTransaction`](../../cash.z.wallet.sdk.entity/-pending-transaction/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Attempts to cancel a transaction that is about to be sent. Typically, cancellation is only an option if the transaction has not yet been submitted to the server. |
| [getAddress](get-address.md) | `suspend fun getAddress(accountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Gets the address for the given account. |
| [isValidShieldedAddr](is-valid-shielded-addr.md) | `suspend fun isValidShieldedAddr(address: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true when the given address is a valid z-addr. Invalid addresses will throw an exception. Valid z-addresses have these characteristics: //TODO |
| [isValidTransparentAddr](is-valid-transparent-addr.md) | `suspend fun isValidTransparentAddr(address: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true when the given address is a valid t-addr. Invalid addresses will throw an exception. Valid t-addresses have these characteristics: //TODO |
| [refreshBalance](refresh-balance.md) | `suspend fun refreshBalance(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [sendToAddress](send-to-address.md) | `fun sendToAddress(spendingKey: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, zatoshi: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fromAccountIndex: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): Flow<`[`PendingTransaction`](../../cash.z.wallet.sdk.entity/-pending-transaction/index.md)`>`<br>Sends zatoshi. |
| [start](start.md) | `fun start(parentScope: CoroutineScope?): `[`Synchronizer`](../-synchronizer/index.md)<br>Starts this synchronizer within the given scope. For simplicity, attempting to start an instance that has already been started will throw a [SynchronizerException.FalseStart](../../cash.z.wallet.sdk.exception/-synchronizer-exception/-false-start.md) exception. This reduces the complexity of managing resources that must be recycled. Instead, each synchronizer is designed to have a long lifespan and should be started from an activity, application or session. |
| [stop](stop.md) | `fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Stop this synchronizer and all of its child jobs. Once a synchronizer has been stopped it should not be restarted and attempting to do so will result in an error. Also, this function will throw an exception if the synchronizer was never previously started. |
| [validateAddress](validate-address.md) | `suspend fun validateAddress(address: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Synchronizer.AddressType`](../-synchronizer/-address-type/index.md)<br>Validates the given address, returning information about why it is invalid. |
