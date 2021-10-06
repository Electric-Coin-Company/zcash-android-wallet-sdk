Troubleshooting Migrations
==========

Upcoming Migrating to Version 1.4.* from 1.3.*
--------------------------------------
`ZcashNetwork` is now an interface, with a concrete implementation provided by `NetworkType`.  To migrate, replace calls to `ZcashNetwork.Mainnet` with `NetworkType.Mainnet` or `ZcashNetwork.Testnet` with `NetworkType.Testnet`.

Migrating to Version 1.3.* from 1.2.*
--------------------------------------
The biggest breaking changes in 1.3 that inspired incrementing the minor version number was simplifying down to one "network aware" library rather than two separate libraries, each dedicated to either testnet or mainnet. This greatly simplifies the gradle configuration and has lots of other benefits. Wallets can now set a network with code similar to the following:

```kotlin
// Simple example
val network: ZcashNetwork = if (testMode) ZcashNetwork.Testnet else ZcashNetwork.Mainnet

// Dependency Injection example
@Provides @Singleton fun provideNetwork(): ZcashNetwork = ZcashNetwork.Mainnet
```
1.3 also adds a runtime check for wallets that are accessing properties before the synchronizer has started. By introducing a `prepare` step, we are now able to catch these errors proactively rather than allowing them to turn into subtle bugs that only surface later. We found this when code was accessing properties before database migrations completed, causing undefined results. Developers do not need to make any changes to enable these checks, they happen automatically and result in detailed error messages.

| Error                           | Issue                               | Fix                      |
| ------------------------------- | ----------------------------------- | ------------------------ |
| No value passed for parameter 'network' | Many functions are now network-aware | pass an instance of ZcashNetwork, which is typically set during initialization |
| Unresolved reference: validate  | The `validate` package was removed  | instead of `cash.z.ecc.android.sdk.validate.AddressType`<br/>import `cash.z.ecc.android.sdk.type.AddressType`  |
| Unresolved reference: WalletBalance | WalletBalance was moved out of `CompactBlockProcessor` and up to the `type` package  | instead of `cash.z.ecc.android.sdk.CompactBlockProcessor.WalletBalance`<br/>import `cash.z.ecc.android.sdk.type.WalletBalance`  |
| Unresolved reference: server  | This was replaced by `setNetwork` | instead of `config.server(host, port)`<br/>use `config.setNetwork(network, host, port)` |
| Unresolved reference: balances  | 3 types of balances are now exposed | change `balances` to `saplingBalances` |
| Unresolved reference: latestBalance  | There are now multiple balance types so this convenience function was removed in favor of forcing wallets to think about which balances they want to show.  | In most cases, just use `synchronizer.saplingBalances.value` directly, instead |
| Type mismatch: inferred type is String but ZcashNetwork was expected  | This function is now network aware | use `Initializer.erase(context, network, alias)` |
| Type mismatch: inferred type is Int? but ZcashNetwork was expected | This function is now network aware | use `WalletBirthdayTool.loadNearest(context, network, height)` instead |
| None of the following functions can be called with the arguments supplied: <br/>public open fun deriveShieldedAddress(seed: ByteArray, network: ZcashNetwork, accountIndex: Int = ...): String defined in cash.z.ecc.android.sdk.tool.DerivationTool.Companion<br/>public open fun deriveShieldedAddress(viewingKey: String, network: ZcashNetwork): String defined in cash.z.ecc.android.sdk.tool.DerivationTool.Companion | This function is now network aware | use `deriveShieldedAddress(seed, network)`|
