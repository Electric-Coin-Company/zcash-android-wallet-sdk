[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.secure](../index.md) / [Wallet](index.md) / [sendBalanceInfo](./send-balance-info.md)

# sendBalanceInfo

`suspend fun sendBalanceInfo(accountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = accountIds[0]): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Calculates the latest balance info and emits it into the balance channel. Defaults to the first account.

### Parameters

`accountId` - the account to check for balance info.