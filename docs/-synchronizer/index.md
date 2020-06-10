[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk](../index.md) / [Synchronizer](./index.md)

# Synchronizer

`interface Synchronizer`

Primary interface for interacting with the SDK. Defines the contract that specific
implementations like [MockSynchronizer](#) and [SdkSynchronizer](../-sdk-synchronizer/index.md) fulfill. Given the language-level
support for coroutines, we favor their use in the SDK and incorporate that choice into this
contract.

### Types

| Name | Summary |
|---|---|
| [Status](-status/index.md) | Represents the status of this Synchronizer, which is useful for communicating to the user.`enum class Status` |

### Properties

| Name | Summary |
|---|---|
| [balances](balances.md) | A stream of balance values, separately reflecting both the available and total balance.`abstract val balances: Flow<WalletBalance>` |
| [clearedTransactions](cleared-transactions.md) | A flow of all the transactions that are on the blockchain.`abstract val clearedTransactions: Flow<PagedList<`[`ConfirmedTransaction`](../../cash.z.ecc.android.sdk.db.entity/-confirmed-transaction/index.md)`>>` |
| [latestBalance](latest-balance.md) | An in-memory reference to the most recently calculated balance.`abstract val latestBalance: WalletBalance` |
| [latestHeight](latest-height.md) | An in-memory reference to the latest height seen on the network.`abstract val latestHeight: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [onChainErrorHandler](on-chain-error-handler.md) | A callback to invoke whenever a chain error is encountered. These occur whenever the processor detects a missing or non-chain-sequential block (i.e. a reorg).`abstract var onChainErrorHandler: ((`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`) -> `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`)?` |
| [onCriticalErrorHandler](on-critical-error-handler.md) | Gets or sets a global error handler. This is a useful hook for handling unexpected critical errors.`abstract var onCriticalErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?` |
| [onProcessorErrorHandler](on-processor-error-handler.md) | An error handler for exceptions during processing. For instance, a block might be missing or a reorg may get mishandled or the database may get corrupted.`abstract var onProcessorErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?` |
| [onSubmissionErrorHandler](on-submission-error-handler.md) | An error handler for exceptions while submitting transactions to lightwalletd. For instance, a transaction may get rejected because it would be a double-spend or the user might lose their cellphone signal.`abstract var onSubmissionErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?` |
| [pendingTransactions](pending-transactions.md) | A flow of all the outbound pending transaction that have been sent but are awaiting confirmations.`abstract val pendingTransactions: Flow<`[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`PendingTransaction`](../../cash.z.ecc.android.sdk.db.entity/-pending-transaction/index.md)`>>` |
| [processorInfo](processor-info.md) | A flow of processor details, updated every time blocks are processed to include the latest block height, blocks downloaded and blocks scanned. Similar to the [progress](progress.md) flow but with a lot more detail.`abstract val processorInfo: Flow<ProcessorInfo>` |
| [progress](progress.md) | A flow of progress values, typically corresponding to this Synchronizer downloading blocks. Typically, any non- zero value below 100 indicates that progress indicators can be shown and a value of 100 signals that progress is complete and any progress indicators can be hidden.`abstract val progress: Flow<`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`>` |
| [receivedTransactions](received-transactions.md) | A flow of all transactions related to receiving funds.`abstract val receivedTransactions: Flow<PagedList<`[`ConfirmedTransaction`](../../cash.z.ecc.android.sdk.db.entity/-confirmed-transaction/index.md)`>>` |
| [sentTransactions](sent-transactions.md) | A flow of all transactions related to sending funds.`abstract val sentTransactions: Flow<PagedList<`[`ConfirmedTransaction`](../../cash.z.ecc.android.sdk.db.entity/-confirmed-transaction/index.md)`>>` |
| [status](status.md) | A flow of values representing the [Status](-status/index.md) of this Synchronizer. As the status changes, a new value will be emitted by this flow.`abstract val status: Flow<Status>` |

### Functions

| Name | Summary |
|---|---|
| [cancelSpend](cancel-spend.md) | Attempts to cancel a transaction that is about to be sent. Typically, cancellation is only an option if the transaction has not yet been submitted to the server.`abstract suspend fun cancelSpend(transaction: `[`PendingTransaction`](../../cash.z.ecc.android.sdk.db.entity/-pending-transaction/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [getAddress](get-address.md) | Gets the address for the given account.`abstract suspend fun getAddress(accountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 0): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [getServerInfo](get-server-info.md) | Convenience function that exposes the underlying server information, like its name and consensus branch id. Most wallets should already have a different source of truth for the server(s) with which they operate and thereby not need this function.`abstract suspend fun getServerInfo(): <ERROR CLASS>` |
| [isValidShieldedAddr](is-valid-shielded-addr.md) | Returns true when the given address is a valid z-addr. Invalid addresses will throw an exception. Valid z-addresses have these characteristics: //TODO copy info from related ZIP`abstract suspend fun isValidShieldedAddr(address: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isValidTransparentAddr](is-valid-transparent-addr.md) | Returns true when the given address is a valid t-addr. Invalid addresses will throw an exception. Valid t-addresses have these characteristics: //TODO copy info from related ZIP`abstract suspend fun isValidTransparentAddr(address: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [sendToAddress](send-to-address.md) | Sends zatoshi.`abstract fun sendToAddress(spendingKey: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, zatoshi: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = "", fromAccountIndex: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 0): Flow<`[`PendingTransaction`](../../cash.z.ecc.android.sdk.db.entity/-pending-transaction/index.md)`>` |
| [start](start.md) | Starts this synchronizer within the given scope.`abstract fun start(parentScope: CoroutineScope? = null): `[`Synchronizer`](./index.md) |
| [stop](stop.md) | Stop this synchronizer. Implementations should ensure that calling this method cancels all jobs that were created by this instance.`abstract fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [validateAddress](validate-address.md) | Validates the given address, returning information about why it is invalid. This is a convenience method that combines the behavior of [isValidShieldedAddr](is-valid-shielded-addr.md) and [isValidTransparentAddr](is-valid-transparent-addr.md) into one call so that the developer doesn't have to worry about handling the exceptions that they throw. Rather, exceptions are converted to [AddressType.Invalid](../../cash.z.ecc.android.sdk.validate/-address-type/-invalid/index.md) which has a `reason` property describing why it is invalid.`abstract suspend fun validateAddress(address: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`AddressType`](../../cash.z.ecc.android.sdk.validate/-address-type/index.md) |
| [validateConsensusBranch](validate-consensus-branch.md) | Validate whether the server and this SDK share the same consensus branch. This is particularly important to check around network updates so that any wallet that's connected to an incompatible server can surface that information effectively. For the SDK, the consensus branch is used when creating transactions as each one needs to target a specific branch. This function compares the server's branch id to this SDK's and returns information that helps determine whether they match.`abstract suspend fun validateConsensusBranch(): `[`ConsensusMatchType`](../../cash.z.ecc.android.sdk.validate/-consensus-match-type/index.md) |

### Inheritors

| Name | Summary |
|---|---|
| [SdkSynchronizer](../-sdk-synchronizer/index.md) | A Synchronizer that attempts to remain operational, despite any number of errors that can occur. It acts as the glue that ties all the pieces of the SDK together. Each component of the SDK is designed for the potential of stand-alone usage but coordinating all the interactions is non- trivial. So the Synchronizer facilitates this, acting as reference that demonstrates how all the pieces can be tied together. Its goal is to allow a developer to focus on their app rather than the nuances of how Zcash works.`class SdkSynchronizer : `[`Synchronizer`](./index.md) |
