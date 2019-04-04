[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [MockSynchronizer](index.md) / [&lt;init&gt;](./-init-.md)

# &lt;init&gt;

`MockSynchronizer(transactionInterval: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)` = 30_000L, initialLoadDuration: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)` = 5_000L, activeTransactionUpdateFrequency: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)` = 3_000L, isFirstRun: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = Random.nextBoolean(), isStale: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`? = null, onSynchronizerErrorListener: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)? = null)`

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