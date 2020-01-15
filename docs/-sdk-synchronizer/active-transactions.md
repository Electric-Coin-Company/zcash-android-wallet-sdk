[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [SdkSynchronizer](index.md) / [activeTransactions](./active-transactions.md)

# activeTransactions

`fun activeTransactions(): ReceiveChannel<`[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`ActiveTransaction`](../-active-transaction/index.md)`, `[`TransactionState`](../-transaction-state/index.md)`>>`

Overrides [Synchronizer.activeTransactions](../-synchronizer/active-transactions.md)

A stream of all the wallet transactions, delegated to the [activeTransactionManager](#).

