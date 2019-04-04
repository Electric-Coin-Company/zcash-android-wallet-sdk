[zcash-android-wallet-sdk](../../../index.md) / [cash.z.wallet.sdk.secure](../../index.md) / [Wallet](../index.md) / [WalletBirthday](index.md) / [tree](./tree.md)

# tree

`val tree: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

the sapling tree corresponding to the given height. This takes around 15 minutes of processing to
generate from scratch because all blocks since activation need to be considered. So when it is calculated in
advance it can save the user a lot of time.

