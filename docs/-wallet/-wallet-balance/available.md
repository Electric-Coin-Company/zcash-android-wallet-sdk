[zcash-android-wallet-sdk](../../../index.md) / [cash.z.wallet.sdk.secure](../../index.md) / [Wallet](../index.md) / [WalletBalance](index.md) / [available](./available.md)

# available

`val available: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)

the amount of funds that are available for use. Typical reasons that funds may be unavailable
include fairly new transactions that do not have enough confirmations or notes that are tied up because we are
awaiting change from a transaction. When a note has been spent, its change cannot be used until there are enough
confirmations.

