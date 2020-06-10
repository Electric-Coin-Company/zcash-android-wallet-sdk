[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk](../index.md) / [Synchronizer](./index.md)

# Synchronizer

`interface Synchronizer`

Primary interface for interacting with the SDK. Defines the contract that specific
implementations like [MockSynchronizer](#) and [SdkSynchronizer](../-sdk-synchronizer/index.md) fulfill. Given the language-level
support for coroutines, we favor their use in the SDK and incorporate that choice into this
contract.

### Types

| Name | Summary |
|---|---|
| [AddressType](-address-type/index.md) | `sealed class AddressType` |
| [Status](-status/index.md) | `enum class Status` |

### Properties

| Name | Summary |
|---|---|
| [balances](balances.md) | `abstract val balances: Flow<`[`CompactBlockProcessor.WalletBalance`](../../cash.z.ecc.android.sdk.block/-compact-block-processor/-wallet-balance/index.md)`>`<br>A stream of balance values, separately reflecting both the available and total balance. |
| [clearedTransactions](cleared-transactions.md) | `abstract val clearedTransactions: Flow<PagedList<`[`ConfirmedTransaction`](../../cash.z.ecc.android.sdk.entity/-confirmed-transaction/index.md)`>>`<br>A flow of all the transactions that are on the blockchain. |
| [onCriticalErrorHandler](on-critical-error-handler.md) | `abstract var onCriticalErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>Gets or sets a global error handler. This is a useful hook for handling unexpected critical errors. |
| [onProcessorErrorHandler](on-processor-error-handler.md) | `abstract var onProcessorErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>An error handler for exceptions during processing. For instance, a block might be missing or a reorg may get mishandled or the database may get corrupted. |
| [onSubmissionErrorHandler](on-submission-error-handler.md) | `abstract var onSubmissionErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`<br>An error handler for exceptions while submitting transactions to lightwalletd. For instance, a transaction may get rejected because it would be a double-spend or the user might lose their cellphone signal. |
| [pendingTransactions](pending-transactions.md) | `abstract val pendingTransactions: Flow<`[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`PendingTransaction`](../../cash.z.ecc.android.sdk.entity/-pending-transaction/index.md)`>>`<br>A flow of all the outbound pending transaction that have been sent but are awaiting confirmations. |
| [processorInfo](processor-info.md) | `abstract val processorInfo: Flow<`[`CompactBlockProcessor.ProcessorInfo`](../../cash.z.ecc.android.sdk.block/-compact-block-processor/-processor-info/index.md)`>`<br>A flow of processor details, updated every time blocks are processed to include the latest block height, blocks downloaded and blocks scanned. Similar to the [progress](progress.md) flow but with a lot more detail. |
| [progress](progress.md) | `abstract val progress: Flow<`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`>`<br>A flow of progress values, typically corresponding to this Synchronizer downloading blocks. Typically, any non- zero value below 100 indicates that progress indicators can be shown and a value of 100 signals that progress is complete and any progress indicators can be hidden. |
| [receivedTransactions](received-transactions.md) | `abstract val receivedTransactions: Flow<PagedList<`[`ConfirmedTransaction`](../../cash.z.ecc.android.sdk.entity/-confirmed-transaction/index.md)`>>`<br>A flow of all transactions related to receiving funds. |
| [sentTransactions](sent-transactions.md) | `abstract val sentTransactions: Flow<PagedList<`[`ConfirmedTransaction`](../../cash.z.ecc.android.sdk.entity/-confirmed-transaction/index.md)`>>`<br>A flow of all transactions related to sending funds. |
| [status](status.md) | `abstract val status: Flow<`[`Synchronizer.Status`](-status/index.md)`>`<br>A flow of values representing the [Status](-status/index.md) of this Synchronizer. As the status changes, a new value will be emitted by this flow. |

### Functions

| Name | Summary |
|---|---|
| [cancelSpend](cancel-spend.md) | `abstract suspend fun cancelSpend(transaction: `[`PendingTransaction`](../../cash.z.ecc.android.sdk.entity/-pending-transaction/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Attempts to cancel a transaction that is about to be sent. Typically, cancellation is only an option if the transaction has not yet been submitted to the server. |
| [getAddress](get-address.md) | `abstract suspend fun getAddress(accountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 0): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Gets the address for the given account. |
| [isValidShieldedAddr](is-valid-shielded-addr.md) | `abstract suspend fun isValidShieldedAddr(address: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true when the given address is a valid z-addr. Invalid addresses will throw an exception. Valid z-addresses have these characteristics: //TODO |
| [isValidTransparentAddr](is-valid-transparent-addr.md) | `abstract suspend fun isValidTransparentAddr(address: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true when the given address is a valid t-addr. Invalid addresses will throw an exception. Valid t-addresses have these characteristics: //TODO |
| [sendToAddress](send-to-address.md) | `abstract fun sendToAddress(spendingKey: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, zatoshi: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = "", fromAccountIndex: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 0): Flow<`[`PendingTransaction`](../../cash.z.ecc.android.sdk.entity/-pending-transaction/index.md)`>`<br>Sends zatoshi. |
| [start](start.md) | `abstract fun start(parentScope: CoroutineScope? = null): `[`Synchronizer`](./index.md)<br>Starts this synchronizer within the given scope. |
| [stop](stop.md) | `abstract fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Stop this synchronizer. Implementations should ensure that calling this method cancels all jobs that were created by this instance. |
| [validateAddress](validate-address.md) | `abstract suspend fun validateAddress(address: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Synchronizer.AddressType`](-address-type/index.md)<br>Validates the given address, returning information about why it is invalid. |

### Inheritors

| Name | Summary |
|---|---|
| [SdkSynchronizer](../-sdk-synchronizer/index.md) | `class SdkSynchronizer : `[`Synchronizer`](./index.md)<br>A Synchronizer that attempts to remain operational, despite any number of errors that can occur. It acts as the glue that ties all the pieces of the SDK together. Each component of the SDK is designed for the potential of stand-alone usage but coordinating all the interactions is non- trivial. So the Synchronizer facilitates this, acting as reference that demonstrates how all the pieces can be tied together. Its goal is to allow a developer to focus on their app rather than the nuances of how Zcash works. |
