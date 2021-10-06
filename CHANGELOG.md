Change Log
==========

Upcoming
------------------------------------
- API change: `ZcashNetwork` is now an interface, with a concrete implementation provided by `NetworkType`
- Fix: Testnet now works correctly #270

Version 1.3.0-beta18 *(2021-08-10)*
------------------------------------
- Fix: Corrected logic when calculating birthdates for wallets with zero received notes.


Version 1.3.0-beta17 *(2021-07-29)*
------------------------------------
- Fix: Autoshielding confirmation count error so funds are available after 10 confirmations.
- New: Allow developers to enable Rust logs.
- New: Accept GZIP compression from lightwalletd.
- New: Reduce the UTXO retry time.

Version 1.3.0-beta16 *(2021-06-30)*
------------------------------------
- Fix: Gracefully handle failures while fetching UTXOs.
- New: Expose StateFlows for balances.
- New: Make it easier to subscribe to transactions.
- New: Cleanup default logs.
- New: Convenience functions for WalletBalance objects.

Version 1.3.0-beta15 *(2021-06-21)*
------------------------------------
- Fix: Increase reconnection attempts on failed app restart.
- New: Updated checkpoints for testnet and mainnet.

Version 1.3.0-beta14 *(2021-06-21)*
------------------------------------
- New: Add separate flows for sapling, orchard and tranparent balances.
- Fix: Continue troubleshooting and fixing server disconnects.
- Updated dependencies.

Version 1.3.0-beta12 *(2021-06-07)*
------------------------------------
- New: Expose network height as StateFlow.
- Fix: Reconnect to lightwalletd when a service exception occurs.

Version 1.3.0-beta11 *(2021-05-12)*
------------------------------------
- Fix: Remove unused flag that was breaking new wallet creation for some wallets.

Version 1.3.0-beta10 *(2021-05-07)*
------------------------------------
- Fix: Make it safe to call the new prepare function more than once.

Version 1.3.0-beta09 *(2021-05-07)*
------------------------------------
- New: Add quick rewind feature, which makes it easy to rescan blocks after an upgrade.
- Fix: Repair complex data migration bug that caused crashes on upgrades.

Version 1.3.0-beta08 *(2021-05-01)*
------------------------------------
- Fix: Disable librustzcash logs by default.

Version 1.3.0-beta07 *(2021-05-01)*
------------------------------------
- Fix: Address issues with key migration, allowing wallets to reset viewing keys, when needed.

Version 1.3.0-beta06 *(2021-04-29)*
------------------------------------
- Fix: Repair publishing so that AARs work on Windows machines [issue #222].
- Fix: Incorrect BranchId on 32-bit devics [issue #224].
- Fix: Rescan should not go beyond the wallet checkpoint.
- New: Drop Android Jetifier since it is no longer used.
- Updated checkpoints, improved tests (added Test Suites) and better error messages.

Version 1.3.0-beta05 *(2021-04-23)*
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

Version 1.2.1-beta04 *(2021-01-05)*
------------------------------------
- New: Updated to latest versions of grpc, grpc-okhttp and protoc
- Fix: Addresses root issue of Android 11 crash on SSL sockets

Version 1.2.1-beta03 *(2020-12-18)*
------------------------------------
- New: Implements ZIP-313, reducing the default fee from 10,000 to 1,000 zats.
- Fix: 80% reduction in build warnings from 90 -> 18 and improved docs [Credit: @herou].

Version 1.2.1-beta02 *(2020-11-24)*
------------------------------------
- New: Improve birthday configuration and config functions.
- Fix: Broken layout in demo app transaction list.

Version 1.2.1-beta01 *(2020-11-19)*
------------------------------------
- New: Added latest checkpoints for testnet and mainnet.
- New: Added display name for Canopy.
- New: Update to the latest lightwalletd service definition.
- Fix: Convert Initializer.Builder to Initializer.Config to simplify the constructors.

Version 1.2.0-beta01 *(2020-10-30)*
------------------------------------
- New: Added ability to erase initializer data.
- Fix: Updated to latest librustzcash, fixing send functionality on Canopy.

Version 1.1.0-beta10 *(2020-10-16)*
------------------------------------
- New: Modified visibility on a few things to facilitate partner integrations.

Version 1.1.0-beta08 *(2020-10-01)*
------------------------------------
- Fix: Publishing has been corrected by jcenter's support team.
- New: Minor improvement to initializer

Version 1.1.0-beta05 *(2020-09-11)*
------------------------------------
- New: Synchronizer can now be started with just a viewing key.
- New: Initializer improvements.
- New: Added tool for loading checkpoints.
- New: Added tool for deriving keys and addresses, statically.
- New: Updated and revamped the demo apps.
- New: Added a bit more (unofficial) t-addr support.
- Fix: Broken testnet demo app.
- Fix: Publishing configuration.

Version 1.1.0-beta04 *(2020-08-13)*
------------------------------------
- New: Add support for canopy on testnet.
- New: Change the default lightwalletd server.
- New: Add lightwalletd service for fetching t-addr transactions.
- New: prove the concept of local RPC via protobufs.
- New: Iterate on the demo app.
- New: Added new checkpoints.
- Fix: Minor enhancements.

Version 1.1.0-beta03 *(2020-08-01)*
------------------------------------
- New: Add robust support for transaction cancellation.
- New: Update to latest version of librustzcash.
- New: Expand test support.
- New: Improve and simplify intialization.
- New: Flag when rust is running in debug mode, causing a 10X slow down.
- New: Contributing guidelines.
- Fix: Minor cleanup and improvements.
