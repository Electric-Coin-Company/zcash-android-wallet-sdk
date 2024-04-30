# Changelog
All notable changes to this library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this library adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- `SdkSynchronizer.closeFlow()` is a Flow-providing version of `Synchronizer.close()`. It safely closes the 
  Synchronizer together with the related components.
- `WalletCoordinator.deleteSdkDataFlow` is a Flow-providing function that deletes all the persisted data in the SDK 
  (databases associated with the wallet, all compact blocks, and data derived from those blocks) but preserves the 
  wallet secrets.

## [2.1.1] - 2024-04-23

### Changed
- The SDK components no longer contain logging statements in the release build
- `safelyConvertToBigDecimal()` API from `CurrencyFormatter.kt` now expects decimal separator Char on input
- Gradle 8.7
- Android Gradle Plugin 8.3.0
- Kotlin 1.9.23
- Other dependencies update
- Checkpoints update

## [2.1.0] - 2024-04-09

### Added
- The Orchard support has been finished, and the SDK now fully supports sending and receiving funds on the Orchard 
  addresses

### Fixed
- SDK release 1.11.0-beta01 documented that `Synchronizer.new` would throw an
  exception indicating that an internal migration requires the wallet seed, if
  called with `null`. This has been unintentionally broken the entire time: the
  handling logic for this case was accidentally removed shortly after it was
  added. The SDK now correctly throws `InitializeException.SeedRequired`.

### Changed
- `Synchronizer.refreshAllBalances` now refreshes the Orchard balances as well
- The SDK uses ZIP-317 fee system internally
- `ZcashSdk.MINERS_FEE` has been deprecated, and will be removed in 2.1.x
- `ZecSend` data class now provides `Proposal?` object initiated using `Synchronizer.proposeTransfer`
- Wallet initialization using `Synchronizer.new` now could throw a new `SeedNotRelevant` exception when the provided 
  seed is not relevant to any of the derived accounts in the wallet database
- Checkpoints update

## [2.0.7] - 2024-03-08

### Fixed
- `Synchronizer.sendToAddress` and `Synchronizer.shieldFunds` now throw an
  exception if the created transaction successfully reaches `lightwalletd` but
  fails to reach its backing full node's mempool.

### Changed
- `WalletBalance` now contains new fields `changePending` and `valuePending`. Fields `total` and `pending` are 
  still provided. See more in the class documentation 
  `sdk-lib/src/main/java/cash/z/ecc/android/sdk/model/WalletBalance.kt`
- `Synchronizer.transparentBalances: WalletBalance` to `Synchronizer.transparentBalance: Zatoshi`
- `WalletSnapshot.transparentBalance: WalletBalance` to `WalletSnapshot.transparentBalance: Zatoshi`
- `Memo.MAX_MEMO_LENGTH_BYTES` is now available in public API
- `Synchronizer.sendToAddress` and `Synchronizer.shieldFunds` have been
  deprecated, and will be removed in 2.1.x (which will create multiple
  transactions at once for some recipients).

### Added
- APIs that enable constructing a proposal for transferring or shielding funds,
  and then creating transactions from a proposal. The intermediate proposal can
  be used to determine the required fee, before committing to producing
  transactions.
  - `Synchronizer.proposeTransfer`
  - `Synchronizer.proposeShielding`
  - `Synchronizer.createProposedTransactions`
- `WalletBalanceFixture` class with mock values that are supposed to be used only for testing purposes 
- `Memo.countLength(memoString: String)` to count memo length in bytes
- `PersistableWallet.toSafeString` is a safe alternative for the regular [toString] function that prints only 
  non-sensitive parts
- `Synchronizer.validateServerEndpoint` this function checks whether the provided server endpoint is valid. 
  The validation is based on comparing:
  * network type
  * sapling activation height
  * consensus branch id

## [2.0.6] - 2024-01-30

### Fixed
- In 2.0.5, `Synchronizer.shieldFunds` always returned an error due to a crash
  on the Rust side. This release fixes the underlying bug.

## [2.0.5] - 2024-01-30

### Added
- `cash.z.ecc.android.sdk.model.Proposal` (currently unused in the public API).
- System tracing to `CompactBlockProcessor` and the Rust backend.

### Changed
- Migrated to NDK 26.1.10909125 and Rust 1.75.0.
- The wallet balances are now updated immediately upon synchronizer start.
- Existing wallets will now only fetch the most recent subtree roots, improving
  synchronizer startup times.
- Performance of block scanning and `SdkSynchronizer.refreshAllBalances` has
  been improved.
- `WalletAddressFixture` fixture properties have been updated

### Fixed
- The transparent wallet balance `StateFlow` now shows the total transparent
  balance in the wallet, instead of the balance of the default address. It also
  now treats all zero-conf balance as available.

### Removed
- `SdkSynchronizer.refreshSaplingBalance` and
  `SdkSynchronizer.refreshTransparentBalance`
  (use `SdkSynchronizer.refreshAllBalances` instead).

## [2.0.4] - 2024-01-08

### Added
- `TransactionOverview.txIdString()` to provide a readable transaction ID to SDK-consuming apps
- `MonetarySeparators.current(locale: Locale? = null)` now accepts `Locale` on input to force separators locale. If 
  no value is provided, the default one is used. 

### Removed
- `LightWalletEndpointExt` and its functions and variables were removed from the SDK's public APIs entirely. It's 
  preserved only for testing and wallet Demo app purposes. The calling wallet app should provide its own 
  `LightWalletEndpoint` instance within `PersistableWallet` or `SdkSynchronizer` APIs.

### Changed
- Gradle 8.5
- Kotlin 1.9.21
- Other dependency update
- Checkpoints update

### Removed
- Several internally unused exceptions from `Exceptions.kt`

## [2.0.3] - 2023-11-08

### Added
- `Synchronizer.getExistingDataDbFilePath` public API to check and provide file path to the existing data database 
  file or throws [InitializeException.MissingDatabaseException] if the database doesn't exist yet. See #1292.

### Changed
- `CompactBlockProcessor` switched internally from balance and progress FFIs to wallet summary FFI APIs. This change 
  brings a block synchronization speed up. No action is required on the client side. See #1282.
- Checkpoints update

## [2.0.2] - 2023-10-20

### Fixed
- Incorrect note deduplication in the `v_transactions` database view: This is a fix in the Rust layer. The amount 
  sent in the transaction was incorrectly reported even though the actual amount was correctly sent. Now, clients 
  should see the amount they expect to see.

### Changed
- Checkpoints update

## [2.0.1] - 2023-10-02

### Changed
- `PersistableWallet` API provides a new `endpoint` parameter of type `LightWalletEndpoint`, which could be used for 
  the Lightwalletd server customization. The new parameter is part of PersistableWallet persistence. The SDK handles 
  the persistence migration internally.
- The **1_000** Zatoshi fee proposed in ZIP-313 is deprecated now, so the minimum is **10_000** Zatoshi, defined in 
  ZIP-317—the `ZcashSdk.MINERS_FEE` now returns the correct value as described above. Note that the actual fee is 
  handled in a rust layer.
- Adopted the latest Bip39 library v1.0.6

## [2.0.0] - 2023-09-25

## [2.0.0-rc.4] - 2023-09-22

### Fixed
Transparent balance is now correctly updated after a shielding transaction is
created, instead of only once the transaction is mined.

## [2.0.0-rc.3] - 2023-09-21

### Fixed
The Kotlin layer of the SDK now correctly matches the Rust layer `PrevHashMismatch` exception with `ContinuityError` 
and triggers rewind action. 

## [2.0.0-rc.2] - 2023-09-20

### Changed
- Some of the `TransactionOverview` class parameters changed:
  - `id` was removed
  - `index` is nullable
  - `feePaid` is nullable
  - `blockTimeEpochSeconds` is nullable

### Removed
- Block heights are absolute, not relative. Thus, these two operations above the `BlockHeight` object were removed:
  - `plus(other: BlockHeight): BlockHeight`
  - `minus(other: BlockHeight): BlockHeight`

## [2.0.0-rc.1] - 2023-09-12

### Notable Changes

- `CompactBlockProcessor` now processes compact blocks from the lightwalletd
  server using the **Spend-before-Sync** algorithm, which allows scanning of
  wallet blocks to be performed in arbitrary order and optimized to make it
  possible to spend received notes without waiting for synchronization to be
  complete. This feature shortens the time until a wallet's spendable balance
  can be used.
- The block synchronization mechanism is additionally about one-third faster
  thanks to an optimized `CompactBlockProcessor.SYNC_BATCH_SIZE` (issue **#1206**).

### Removed
- `CompactBlockProcessor.ProcessorInfo.lastSyncHeight` no longer had a
  well-defined meaning after implementation of the **SpendBeforeSync**
  synchronization algorithm and has been removed.
  `CompactBlockProcessor.ProcessorInfo.overallSyncRange` provides related
  information.
- `CompactBlockProcessor.ProcessorInfo.isSyncing`. Use `Synchronizer.status` instead.
- `CompactBlockProcessor.ProcessorInfo.syncProgress`. Use `Synchronizer.progress` instead.
- `alsoClearBlockCache` parameter from rewind functions of `Synchronizer` and
  `CompactBlockProcessor`, as it has no effect on the current behaviour of
  these functions.
- Internally, we removed access to the shared block table from the Kotlin
  layer, which resulted in eliminating these APIs:
  - `SdkSynchronizer.findBlockHash()`
  - `SdkSynchronizer.findBlockHashAsHex()`

### Changed
- `CompactBlockProcessor.quickRewind()` and `CompactBlockProcessor.rewindToNearestHeight()`
  now might fail due to internal changes in getting scanned height. Thus, these
  functions now return `Boolean` results.
- `Synchronizer.new()` and `PersistableWallet` APIs require a new
  `walletInitMode` parameter of type `WalletInitMode`, which describes wallet
  initialization mode. See related function and sealed class documentation.

### Fixed
- `Synchronizer.getMemos()` now correctly returns a flow of strings for sent
  and received transactions. Issue **#1154**.
- `CompactBlockProcessor` now triggers transaction polling while block
  synchronization is in progress as expected. Clients will be notified shortly
  after every new transaction is discovered via `Synchronizer.transactions`
  API. Issue **#1170**.

## [1.21.0-beta01]

Note: This is the last _1.x_ version release. The upcoming version _2.0_ brings the **Spend-before-Sync** feature,
which speeds up discovering the wallet's spendable balance.

### Changed
- Updated dependencies:
   - Gradle 8.3
   - AGP 8.1.1
   - Kotlin 1.9.10
   - Coroutines 1.7.3
   - Compose
   - AndroidX
   - gRPC/Protobuf
   - etc.
- Checkpoints

## 1.20.0-beta01
- The SDK internally migrated from `BackendExt` rust backend extension functions to more type-safe `TypesafeBackend`.
- `Synchronizer.getMemos()` now internally handles expected `RuntimeException` from the rust layer and transforms it
  in an empty string.

## 1.19.0-beta01
### Changed
- Adopted the latest Bip39 version 1.0.5

### Fixed
- `TransactionOverview` object returned with `SdkSynchronizer.transactions` now contains a correct `TransactionState.
  Pending` in case of the transaction is mined,but not fully confirmed.
- When the SDK internally works with a recently created transaction there was a moment in which could the transaction
  causes the SDK to crash, because of its invalid mined height. Fixed now.

## 1.18.0-beta01
- Synchronizer's functions `getUnifiedAddress`, `getSaplingAddress`, `getTransparentAddress`, and `refreshUtxos` now
  do not provide `Account.DEFAULT` value for the account argument. As accounts are not fully supported by the SDK
  yet, the caller should explicitly set Account.DEFAULT as the account argument to keep the same behavior.
- Gradle 8.1.1
- AGP 8.0.2

## 1.17.0-beta01
- Transparent fund balances are now displayed almost immediately
- Synchronization of shielded balances and transaction history is about 30% faster
- Disk space usage is reduced by about 90%
- `Synchronizer.status` has been simplified by combining `DOWNLOADING`, `VALIDATING`, and `SCANNING` states into a single `SYNCING` state.
- `Synchronizer.progress` now returns `Flow<PercentDecimal>` instead of `Flow<Int>`. PercentDecimal is a type-safe model. Use `PercentDecimal.toPercentage()` to get a number within 0-100% scale.
- `Synchronizer.clearedTransactions` has been renamed to `Synchronizer.transactions` and includes sent, received, and pending transactions.  Synchronizer APIs for listing sent, received, and pending transactions have been removed.  Clients can determine whether a transaction is sent, received, or pending by filtering the `TransactionOverview` objects returned by `Synchronizer.transactions`
- `Synchronizer.send()` and `shieldFunds()` are now `suspend` functions with `Long` return values representing the ID of the newly created transaction.  Errors are reported by thrown exceptions.
 - `DerivationTool` is now an interface, rather than an `object`, which makes it easier to inject alternative implementations into tests.  To adapt to the new API, replace calls to `DerivationTool.methodName()` with `DerivationTool.getInstance().methodName()`.
 - `DerivationTool` methods are no longer suspending, which should make it easier to call them in various situations.  Obtaining a `DerivationTool` instance via `DerivationTool.getInstance()` frontloads the need for a suspending call.
 - `DerivationTool.deriveUnifiedFullViewingKeys()` no longer has a default argument for `numberOfAccounts`.  Clients should now pass `DerivationTool.DEFAULT_NUMBER_OF_ACCOUNTS` as the value. Note that the SDK does not currently have proper support for multiple accounts.
 - The SDK's internals for connecting with librustzcash have been refactored to a separate Gradle module `backend-lib` (and therefore a separate artifact) which is a transitive dependency of the Zcash Android SDK.  SDK consumers that use Gradle dependency locks may notice this difference, but otherwise it should be mostly an invisible change.

## 1.16.0-beta01
(This version was only deployed as a snapshot and not released on Maven Central)
### Changed
 - The minimum supported version of Android is now API level 27.

## 1.15.0-beta01
### Changed
- A new package `sdk-incubator-lib` is now available as a public API.  This package contains experimental APIs that may be promoted to the SDK in the future.  The APIs in this package are not guaranteed to be stable, and may change at any time.
- `Synchronizer.refreshUtxos` now takes `Account` type as first parameter instead of transparent address of type
    `String`, and thus it downloads all UTXOs for the given account addresses. The Account object provides a default `0` index Account with `Account.DEFAULT`.

## 1.14.0-beta01
### Changed
 - The minimum supported version of Android is now API level 24.

## 1.13.0-beta01
### Changed
- The SDK's internal networking has been refactored to a separate Gradle module `lightwallet-client-lib` (and
  therefore a separate artifact) which is a transitive dependency of the Zcash Android SDK.
    - The `z.cash.ecc.android.sdk.model.LightWalletEndpoint` class has been moved to `co.electriccoin.lightwallet.client.model.LightWalletEndpoint`
    - The new networking module now provides a `LightWalletClient` for asynchronous calls.
    - Most unary calls respond with the new `Response` class and its subclasses. Streaming calls will be updated
      with the Response class later.
    - SDK clients should avoid using generated GRPC objects, as these are an internal implementation detail and are in process of being removed from the public API.  Any clients using GRPC objects will find these have been repackaged from `cash.z.wallet.sdk.rpc` to `cash.z.wallet.sdk.internal.rpc` to signal they are not a public API.

## 1.12.0-beta01
### Changed
 - `TransactionOverview`, `Transaction.Sent`, and `Transaction.Received` have `minedHeight` as a nullable field now.  This fixes a potential crash when fetching transactions when a transaction is in the mempool

## 1.11.0-beta01
### Added
- `cash.z.ecc.android.sdk`:
  - `Synchronizer.getUnifiedAddress`
  - `Synchronizer.getSaplingAddress`
  - `Synchronizer.isValidUnifiedAddr`
  - `Synchronizer.getMemos(TransactionOverview)`
  - `Synchronizer.getReceipients(TransactionOverview)`
- `cash.z.ecc.android.sdk.model`:
  - `Account`
  - `FirstClassByteArray`
  - `PendingTransaction`
  - `Transaction`
  - `UnifiedSpendingKey`
- `cash.z.ecc.android.sdk.tool`:
  - `DerivationTool.deriveUnifiedSpendingKey`
  - `DerivationTool.deriveUnifiedFullViewingKey`
  - `DerivationTool.deriveTransparentAccountPrivateKey`
  - `DerivationTool.deriveTransparentAddressFromAccountPrivateKey`
  - `DerivationTool.deriveUnifiedAddress`
  - `DerivationTool.deriveUnifiedFullViewingKeys`
  - `DerivationTool.validateUnifiedFullViewingKey`
    - Still unimplemented.
- `cash.z.ecc.android.sdk.type`:
  - `AddressType.Unified`
  - `UnifiedFullViewingKey`, representing a Unified Full Viewing Key as specified in
    [ZIP 316](https://zips.z.cash/zip-0316#encoding-of-unified-full-incoming-viewing-keys).

### Changed
- The following methods now take or return `UnifiedFullViewingKey` instead of
  `UnifiedViewingKey`:
    - `cash.z.ecc.android.sdk`:
      - `Initializer.Config.addViewingKey`
      - `Initializer.Config.importWallet`
      - `Initializer.Config.newWallet`
      - `Initializer.Config.setViewingKeys`
- `cash.z.ecc.android.sdk`:
  - `Synchronizer.Companion.new` now takes many of the arguments previously passed to `Initializer`. In addition, an optional `seed` argument is required for first-time initialization or if `Synchronizer.new` throws an exception indicating that an internal migration requires the wallet seed.  (This second case will be true the first time existing clients upgrade to this new version of the SDK).
  - `Synchronizer.new()` now returns an instance that implements the `Closeable` interface.  `Synchronizer.stop()` is effectively renamed to `Synchronizer.close()`
  - `Synchronizer` ensures that multiple instances cannot be running concurrently with the same network and alias
  - `Synchronizer.sendToAddress` now takes a `UnifiedSpendingKey` instead of an encoded
    Sapling extended spending key, and the `fromAccountIndex` argument is now implicit in
    the `UnifiedSpendingKey`.
  - `Synchronizer.shieldFunds` now takes a `UnifiedSpendingKey` instead of separately
    encoded Sapling and transparent keys.
  - `Synchronizer` methods that previously took an `Int` for account index now take an `Account` object
  - `Synchronizer.sendToAddress()` and `Synchronizer.shieldFunds()` return flows that can now be collected multiple times.  Prior versions of the SDK had a bug that could submit transactions multiple times if the flow was collected more than once.
- Updated dependencies:
  - Kotlin 1.7.21
  - AndroidX
  - etc.
- Updated checkpoints

### Removed
- `cash.z.ecc.android.sdk`:
  - `Initializer` (use `Synchronizer.new` instead)
  - `Synchronizer.start()` - Synchronizer is now started automatically when constructing a new instance.
  - `Synchronizer.getAddress` (use `Synchronizer.getUnifiedAddress` instead).
  - `Synchronizer.getShieldedAddress` (use `Synchronizer.getSaplingAddress` instead)
  - `Synchronizer.cancel`
  - `Synchronizer.cancelSpend`
- `cash.z.ecc.android.sdk.type.UnifiedViewingKey`
  - This type had a bug where the `extpub` field actually was storing a plain transparent
    public key, and not the extended public key as intended. This made it incompatible
    with ZIP 316.
- `cash.z.ecc.android.sdk.tool`:
  - `DerivationTool.deriveSpendingKeys` (use `DerivationTool.deriveUnifiedSpendingKey` instead)
  - `DerivationTool.deriveViewingKey` (use `DerivationTool.deriveUnifiedFullViewingKey` instead)
  - `DerivationTool.deriveTransparentAddress` (use `Synchronizer.getLegacyTransparentAddress` instead).
  - `DerivationTool.deriveTransparentAddressFromPrivateKey` (use `Synchronizer.getLegacyTransparentAddress` instead).
  - `DerivationTool.deriveTransparentAddressFromPublicKey` (use `Synchronizer.getLegacyTransparentAddress` instead).
  - `DerivationTool.deriveTransparentSecretKey` (use `DerivationTool.deriveUnifiedSpendingKey` instead).
  - `DerivationTool.deriveShieldedAddress`
  - `DerivationTool.deriveUnifiedViewingKeys` (use `DerivationTool.deriveUnifiedFullViewingKey` instead)
  - `DerivationTool.validateUnifiedViewingKey`

## Version 1.9.0-beta05
- The minimum version of Android supported is now API 21
- Fixed R8/ProGuard consumer rule, which eliminates a runtime crash for minified apps

## Version 1.9.0-beta04
- The SDK now stores sapling param files in `no_backup/co.electricoin.zcash` folder instead of the `cache/params`
  folder. Besides that, `SaplingParamTool` also does validation of downloaded sapling param file hash and size.
**No action required from client app**.

## Version 1.9.0-beta03
- No changes; this release is a test of a new deployment process

## Version 1.9.0-beta02
- The SDK now stores database files in `no_backup/co.electricoin.zcash` folder instead of the `database` folder. **No action required from client app**.

## Version 1.9.0-beta01
 - Split `ZcashNetwork` into `ZcashNetwork` and `LightWalletEndpoint` to decouple network and server configuration
 - Gradle 7.5.1
 - Updated checkpoints

## Version 1.8.0-beta01
- Enabled automated unit tests run on the CI server
- Added `BlockHeight` typesafe object to represent block heights
- Significantly reduced memory usage, fixing potential OutOfMemoryError during block download
- Kotlin 1.7.10
- Updated checkpoints

## Version 1.7.0-beta01
- Added `Zatoshi` typesafe object to represent amounts.
- Kotlin 1.7.0

## Version 1.6.0-beta01
- Updated checkpoints for Mainnet and Testnet
- Fix: SDK can now be used on Intel x86_64 emulators
- Prevent R8 warnings for apps consuming the SDK

## Version 1.5.0-beta01
- New: Transactions can be created after NU5 activation.
- New: Support for receiving v5 transactions.
- Known issues: The SDK will not run on Intel 64-bit API 31+ emulators.  Workarounds include: testing on a physical device, using an older 32-bit API version Intel emulator, or using an ARM emulator.

## Version 1.4.0-beta01
- Main entrypoint to the SDK has changed.  See [MIGRATIONS.md](MIGRATIONS.md)
- The minimum version of Android supported is now API 19
- Updated checkpoints for Mainnet and Testnet
- Internal bugfixes around concurrent access to resources, which could cause transient failures and data corruption
- Added ProGuard rules so that SDK clients can use R8 to shrink their apps
- Updated dependencies, including Kotlin 1.6.21, Coroutines 1.6.1, GRPC 1.46.0, Okio 3.1.0, NDK 23
- Known issues: The SDK will not run on Intel 64-bit API 31+ emulators.  Workarounds include: testing on a physical device, using an older 32-bit API version Intel emulator, or using an ARM emulator.

## Version 1.3.0-beta20
- New: Updated checkpoints for Mainnet and Testnet

## Version 1.3.0-beta19
- New: Updated checkpoints for Mainnet and Testnet
- Fix: Repackaged internal classes to a new `internal` package name
- Fix: Testnet checkpoints have been corrected
- Updated dependencies

## Version 1.3.0-beta18
- Fix: Corrected logic when calculating birthdates for wallets with zero received notes.

## Version 1.3.0-beta17
- Fix: Autoshielding confirmation count error so funds are available after 10 confirmations.
- New: Allow developers to enable Rust logs.
- New: Accept GZIP compression from lightwalletd.
- New: Reduce the UTXO retry time.

## Version 1.3.0-beta16
- Fix: Gracefully handle failures while fetching UTXOs.
- New: Expose StateFlows for balances.
- New: Make it easier to subscribe to transactions.
- New: Cleanup default logs.
- New: Convenience functions for WalletBalance objects.

## Version 1.3.0-beta15
- Fix: Increase reconnection attempts on failed app restart.
- New: Updated checkpoints for testnet and mainnet.

## Version 1.3.0-beta14
- New: Add separate flows for sapling, orchard and tranparent balances.
- Fix: Continue troubleshooting and fixing server disconnects.
- Updated dependencies.

## Version 1.3.0-beta12
- New: Expose network height as StateFlow.
- Fix: Reconnect to lightwalletd when a service exception occurs.

## Version 1.3.0-beta11
- Fix: Remove unused flag that was breaking new wallet creation for some wallets.

## Version 1.3.0-beta10
- Fix: Make it safe to call the new prepare function more than once.

## Version 1.3.0-beta09
- New: Add quick rewind feature, which makes it easy to rescan blocks after an upgrade.
- Fix: Repair complex data migration bug that caused crashes on upgrades.

## Version 1.3.0-beta08
- Fix: Disable librustzcash logs by default.

## Version 1.3.0-beta07
- Fix: Address issues with key migration, allowing wallets to reset viewing keys, when needed.

## Version 1.3.0-beta06
- Fix: Repair publishing so that AARs work on Windows machines [issue #222].
- Fix: Incorrect BranchId on 32-bit devics [issue #224].
- Fix: Rescan should not go beyond the wallet checkpoint.
- New: Drop Android Jetifier since it is no longer used.
- Updated checkpoints, improved tests (added Test Suites) and better error messages.

## Version 1.3.0-beta05
- Major: Consolidate product flavors into one library for the SDK instead of two.
- Major: Integrates with latest Librustzcash including full Data Access API support.
- Major: Move off of JCenter and onto Maven Central.
- New: Adds Ktlint [Credit: @nighthawk24]
- Fix: Added SaplingParamTool and ability to clear param files from cache [Credit: @herou]
- New: Added responsible disclosure document for vulnerabilities [Credit: @zebambam]
- New: UnifiedViewingKey concept.
- New: Adds support for autoshielding, including database migrations.
- New: Adds basic support for UTXOs, including refresh during scan.
- New: Support the ability to wipe all sqlite data and rebuild from keys.
- New: Switches to ZOMG lightwalletd instances.
- Fix: Only notify subscribers when a new block is detected.
- New: Add scan metrics and callbacks for apps to measure performance.
- Fix: Improve error handling and surface critical Initialization errors.
- New: Adds cleanup and removal of failed transactions.
- New: Improved logic for determining the wallet birthday.
- New: Add the ability to rewind and rescan blocks.
- New: Better safeguards against testnet v mainnet data contamination.
- New: Improved troubleshooting of ungraceful shutdowns.
- Docs: Update README to draw attention to the demo app.
- New: Expose transaction count.
- New: Derive sapling activation height from the active network.
- New: Latest checkpoints for mainnet and testnet.

## Version 1.2.1-beta04
- New: Updated to latest versions of grpc, grpc-okhttp and protoc
- Fix: Addresses root issue of Android 11 crash on SSL sockets

## Version 1.2.1-beta03
- New: Implements ZIP-313, reducing the default fee from 10,000 to 1,000 zats.
- Fix: 80% reduction in build warnings from 90 -> 18 and improved docs [Credit: @herou].

## Version 1.2.1-beta02
- New: Improve birthday configuration and config functions.
- Fix: Broken layout in demo app transaction list.

## Version 1.2.1-beta01
- New: Added latest checkpoints for testnet and mainnet.
- New: Added display name for Canopy.
- New: Update to the latest lightwalletd service definition.
- Fix: Convert Initializer.Builder to Initializer.Config to simplify the constructors.

## Version 1.2.0-beta01
- New: Added ability to erase initializer data.
- Fix: Updated to latest librustzcash, fixing send functionality on Canopy.

## Version 1.1.0-beta10
- New: Modified visibility on a few things to facilitate partner integrations.

## Version 1.1.0-beta08
- Fix: Publishing has been corrected by jcenter's support team.
- New: Minor improvement to initializer

## Version 1.1.0-beta05
- New: Synchronizer can now be started with just a viewing key.
- New: Initializer improvements.
- New: Added tool for loading checkpoints.
- New: Added tool for deriving keys and addresses, statically.
- New: Updated and revamped the demo apps.
- New: Added a bit more (unofficial) t-addr support.
- Fix: Broken testnet demo app.
- Fix: Publishing configuration.

## Version 1.1.0-beta04
- New: Add support for canopy on testnet.
- New: Change the default lightwalletd server.
- New: Add lightwalletd service for fetching t-addr transactions.
- New: prove the concept of local RPC via protobufs.
- New: Iterate on the demo app.
- New: Added new checkpoints.
- Fix: Minor enhancements.

## Version 1.1.0-beta03
- New: Add robust support for transaction cancellation.
- New: Update to latest version of librustzcash.
- New: Expand test support.
- New: Improve and simplify intialization.
- New: Flag when rust is running in debug mode, causing a 10X slow down.
- New: Contributing guidelines.
- Fix: Minor cleanup and improvements.
