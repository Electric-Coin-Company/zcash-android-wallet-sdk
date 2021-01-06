Change Log
==========

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
