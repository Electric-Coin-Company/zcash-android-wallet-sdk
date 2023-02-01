Change Log
==========

## 1.16.0-beta01
### Changed
- `Synchronizer.refreshUtxos` now takes `Account` type as first parameter instead of transparent address of type 
    `String`, and thus it downloads all UTXOs for the given account addresses.

## 1.15.0-beta01
### Changed
- A new package `sdk-incubator-lib` is now available as a public API.  This package contains experimental APIs that may be promoted to the SDK in the future.  The APIs in this package are not guaranteed to be stable, and may change at any time.

## 1.14.0-beta01
### Changed
 - The minimum supported version of Android is now API level 24.

## 1.13.0-beta01
### Changed
- The SDK's internal networking has been refactored to a separate Gradle module `lightwallet-client-lib` (and 
  therefore a separate artifact) which is a transitive dependency of the Zcash Android SDK.
    - The `z.cash.ecc.android.sdk.model.LightWalletEndpoint` class has been moved to `co.electriccoin.lightwallet.client.model.LightWalletEndpoint`
    - The new networking module now provides a `BlockingLightWalletClient` for blocking calls and a 
      `CoroutineLightWalletClient` for asynchronous calls.
    - Most unary calls respond with the new `Response` class and its subclasses. Streaming calls will be updated 
      with the Response class later.

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

Version 1.9.0-beta05
------------------------------------
- The minimum version of Android supported is now API 21
- Fixed R8/ProGuard consumer rule, which eliminates a runtime crash for minified apps

Version 1.9.0-beta04
------------------------------------
- The SDK now stores sapling param files in `no_backup/co.electricoin.zcash` folder instead of the `cache/params` 
  folder. Besides that, `SaplingParamTool` also does validation of downloaded sapling param file hash and size.
**No action required from client app**.

Version 1.9.0-beta03
------------------------------------
- No changes; this release is a test of a new deployment process

Version 1.9.0-beta02
------------------------------------
- The SDK now stores database files in `no_backup/co.electricoin.zcash` folder instead of the `database` folder. **No action required from client app**.

Version 1.9.0-beta01
------------------------------------
 - Split `ZcashNetwork` into `ZcashNetwork` and `LightWalletEndpoint` to decouple network and server configuration
 - Gradle 7.5.1
 - Updated checkpoints

Version 1.8.0-beta01
------------------------------------
- Enabled automated unit tests run on the CI server 
- Added `BlockHeight` typesafe object to represent block heights
- Significantly reduced memory usage, fixing potential OutOfMemoryError during block download
- Kotlin 1.7.10
- Updated checkpoints

Version 1.7.0-beta01
------------------------------------
- Added `Zatoshi` typesafe object to represent amounts.
- Kotlin 1.7.0

Version 1.6.0-beta01
------------------------------------
- Updated checkpoints for Mainnet and Testnet
- Fix: SDK can now be used on Intel x86_64 emulators
- Prevent R8 warnings for apps consuming the SDK

Version 1.5.0-beta01
------------------------------------
- New: Transactions can be created after NU5 activation.
- New: Support for receiving v5 transactions.
- Known issues: The SDK will not run on Intel 64-bit API 31+ emulators.  Workarounds include: testing on a physical device, using an older 32-bit API version Intel emulator, or using an ARM emulator.

Version 1.4.0-beta01
------------------------------------
- Main entrypoint to the SDK has changed.  See [MIGRATIONS.md](MIGRATIONS.md)
- The minimum version of Android supported is now API 19
- Updated checkpoints for Mainnet and Testnet
- Internal bugfixes around concurrent access to resources, which could cause transient failures and data corruption
- Added ProGuard rules so that SDK clients can use R8 to shrink their apps
- Updated dependencies, including Kotlin 1.6.21, Coroutines 1.6.1, GRPC 1.46.0, Okio 3.1.0, NDK 23
- Known issues: The SDK will not run on Intel 64-bit API 31+ emulators.  Workarounds include: testing on a physical device, using an older 32-bit API version Intel emulator, or using an ARM emulator.

Version 1.3.0-beta20
------------------------------------
- New: Updated checkpoints for Mainnet and Testnet

Version 1.3.0-beta19
------------------------------------
- New: Updated checkpoints for Mainnet and Testnet
- Fix: Repackaged internal classes to a new `internal` package name
- Fix: Testnet checkpoints have been corrected
- Updated dependencies

Version 1.3.0-beta18
------------------------------------
- Fix: Corrected logic when calculating birthdates for wallets with zero received notes.

Version 1.3.0-beta17
------------------------------------
- Fix: Autoshielding confirmation count error so funds are available after 10 confirmations.
- New: Allow developers to enable Rust logs.
- New: Accept GZIP compression from lightwalletd.
- New: Reduce the UTXO retry time.

Version 1.3.0-beta16
------------------------------------
- Fix: Gracefully handle failures while fetching UTXOs.
- New: Expose StateFlows for balances.
- New: Make it easier to subscribe to transactions.
- New: Cleanup default logs.
- New: Convenience functions for WalletBalance objects.

Version 1.3.0-beta15
------------------------------------
- Fix: Increase reconnection attempts on failed app restart.
- New: Updated checkpoints for testnet and mainnet.

Version 1.3.0-beta14
------------------------------------
- New: Add separate flows for sapling, orchard and tranparent balances.
- Fix: Continue troubleshooting and fixing server disconnects.
- Updated dependencies.

Version 1.3.0-beta12
------------------------------------
- New: Expose network height as StateFlow.
- Fix: Reconnect to lightwalletd when a service exception occurs.

Version 1.3.0-beta11
------------------------------------
- Fix: Remove unused flag that was breaking new wallet creation for some wallets.

Version 1.3.0-beta10
------------------------------------
- Fix: Make it safe to call the new prepare function more than once.

Version 1.3.0-beta09
------------------------------------
- New: Add quick rewind feature, which makes it easy to rescan blocks after an upgrade.
- Fix: Repair complex data migration bug that caused crashes on upgrades.

Version 1.3.0-beta08
------------------------------------
- Fix: Disable librustzcash logs by default.

Version 1.3.0-beta07
------------------------------------
- Fix: Address issues with key migration, allowing wallets to reset viewing keys, when needed.

Version 1.3.0-beta06
------------------------------------
- Fix: Repair publishing so that AARs work on Windows machines [issue #222].
- Fix: Incorrect BranchId on 32-bit devics [issue #224].
- Fix: Rescan should not go beyond the wallet checkpoint.
- New: Drop Android Jetifier since it is no longer used.
- Updated checkpoints, improved tests (added Test Suites) and better error messages.

Version 1.3.0-beta05
------------------------------------
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

Version 1.2.1-beta04
------------------------------------
- New: Updated to latest versions of grpc, grpc-okhttp and protoc
- Fix: Addresses root issue of Android 11 crash on SSL sockets

Version 1.2.1-beta03
------------------------------------
- New: Implements ZIP-313, reducing the default fee from 10,000 to 1,000 zats.
- Fix: 80% reduction in build warnings from 90 -> 18 and improved docs [Credit: @herou].

Version 1.2.1-beta02
------------------------------------
- New: Improve birthday configuration and config functions.
- Fix: Broken layout in demo app transaction list.

Version 1.2.1-beta01
------------------------------------
- New: Added latest checkpoints for testnet and mainnet.
- New: Added display name for Canopy.
- New: Update to the latest lightwalletd service definition.
- Fix: Convert Initializer.Builder to Initializer.Config to simplify the constructors.

Version 1.2.0-beta01
------------------------------------
- New: Added ability to erase initializer data.
- Fix: Updated to latest librustzcash, fixing send functionality on Canopy.

Version 1.1.0-beta10
------------------------------------
- New: Modified visibility on a few things to facilitate partner integrations.

Version 1.1.0-beta08
------------------------------------
- Fix: Publishing has been corrected by jcenter's support team.
- New: Minor improvement to initializer

Version 1.1.0-beta05
------------------------------------
- New: Synchronizer can now be started with just a viewing key.
- New: Initializer improvements.
- New: Added tool for loading checkpoints.
- New: Added tool for deriving keys and addresses, statically.
- New: Updated and revamped the demo apps.
- New: Added a bit more (unofficial) t-addr support.
- Fix: Broken testnet demo app.
- Fix: Publishing configuration.

Version 1.1.0-beta04
------------------------------------
- New: Add support for canopy on testnet.
- New: Change the default lightwalletd server.
- New: Add lightwalletd service for fetching t-addr transactions.
- New: prove the concept of local RPC via protobufs.
- New: Iterate on the demo app.
- New: Added new checkpoints.
- Fix: Minor enhancements.

Version 1.1.0-beta03
------------------------------------
- New: Add robust support for transaction cancellation.
- New: Update to latest version of librustzcash.
- New: Expand test support.
- New: Improve and simplify intialization.
- New: Flag when rust is running in debug mode, causing a 10X slow down.
- New: Contributing guidelines.
- Fix: Minor cleanup and improvements.
