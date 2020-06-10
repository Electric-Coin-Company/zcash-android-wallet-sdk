[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk.data](../index.md) / [SdkSynchronizer](index.md) / [lastBalance](./last-balance.md)

# lastBalance

`fun lastBalance(): `[`Wallet.WalletBalance`](../../cash.z.ecc.android.sdk.secure/-wallet/-wallet-balance/index.md)

Overrides [Synchronizer.lastBalance](../-synchronizer/last-balance.md)

Holds the most recent value that was transmitted through the [balances](../-synchronizer/balances.md) channel. Typically, if the
underlying channel is a BroadcastChannel (and it should be), then this value is simply [balanceChannel.value](#)

