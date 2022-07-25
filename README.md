[![license](https://img.shields.io/github/license/zcash/zcash-android-wallet-sdk.svg?maxAge=2592000&style=plastic)](https://github.com/zcash/zcash-android-wallet-sdk/blob/master/LICENSE)
![Maven Central](https://img.shields.io/maven-central/v/cash.z.ecc.android/zcash-android-sdk?color=success&style=plastic)

This is a beta build and is currently under active development. Please be advised of the following:

- This code currently is not audited by an external security auditor, use it at your own risk
- The code **has not been subjected to thorough review** by engineers at the Electric Coin Company
- We **are actively changing** the codebase and adding features where/when needed

🔒 Security Warnings

- The Zcash Android Wallet SDK is experimental and a work in progress. Use it at your own risk.
- Developers using this SDK must familiarize themselves with the current [threat
  model](https://zcash.readthedocs.io/en/latest/rtd_pages/wallet_threat_model.html), especially the known weaknesses described there.

---

# Zcash Android SDK

This lightweight SDK connects Android to Zcash. It welds together Rust and Kotlin in a minimal way, allowing third-party Android apps to send and receive shielded transactions easily, securely and privately.

## Contents

- [Requirements](#requirements)
- [Structure](#structure)
- [Overview](#overview)
  - [Components](#components)
- [Quickstart](#quickstart)
- [Examples](#examples)
- [Compiling Sources](#compiling-sources)
- [Versioning](#versioning)
- [Examples](#examples)

## Requirements

This SDK is designed to work with [lightwalletd](https://github.com/zcash-hackworks/lightwalletd)

## Structure

From an app developer's perspective, this SDK will encapsulate the most complex aspects of using Zcash, freeing the developer to focus on UI and UX, rather than scanning blockchains and building commitment trees! Internally, the SDK is structured as follows:

![SDK Diagram](assets/sdk_diagram_final.png?raw=true "SDK Diagram")

Thankfully, the only thing an app developer has to be concerned with is the following:

![SDK Diagram Developer Perspective](assets/sdk_dev_pov_final.png?raw=true "SDK Diagram Dev PoV")

[Back to contents](#contents)

## Overview

At a high level, this SDK simply helps native Android codebases connect to Zcash's Rust crypto libraries without needing to know Rust or be a Cryptographer. Think of it as welding. The SDK takes separate things and tightly bonds them together such that each can remain as idiomatic as possible. Its goal is to make it easy for an app to incorporate shielded transactions while remaining a good citizen on mobile devices.

Given all the moving parts, making things easy requires coordination. The [Synchronizer](docs/-synchronizer/README.md) provides that layer of abstraction so that the primary steps to make use of this SDK are simply:

1. Start the [Synchronizer](docs/-synchronizer/README.md)
2. Subscribe to wallet data

The [Synchronizer](docs/-synchronizer/README.md) takes care of

    - Connecting to the light wallet server
    - Downloading the latest compact blocks in a privacy-sensitive way
    - Scanning and trial decrypting those blocks for shielded transactions related to the wallet
    - Processing those related transactions into useful data for the UI
    - Sending payments to a full node through [lightwalletd](https://github.com/zcash/lightwalletd)
    - Monitoring sent payments for status updates

To accomplish this, these responsibilities of the SDK are divided into separate components. Each component is coordinated by the [Synchronizer](docs/-synchronizer/README.md), which is the thread that ties it all together.

#### Components

| Component                      | Summary                                                                                   |
| :----------------------------- | :---------------------------------------------------------------------------------------- |
| **LightWalletService**         | Service used for requesting compact blocks                                                |
| **CompactBlockStore**          | Stores compact blocks that have been downloaded from the `LightWalletService`             |
| **CompactBlockProcessor**      | Validates and scans the compact blocks in the `CompactBlockStore` for transaction details |
| **OutboundTransactionManager** | Creates, Submits and manages transactions for spending funds                              |
| **Initializer**                | Responsible for all setup that must happen before synchronization can begin. Loads the rust library and helps initialize databases.           |
| **DerivationTool**, **BirthdayTool**                | Utilities for deriving keys, addresses and loading wallet checkpoints, called "birthdays."           |
| **RustBackend**                | Wraps and simplifies the rust library and exposes its functionality to the Kotlin SDK |

[Back to contents](#contents)

## Quickstart

Add flavors for testnet v mainnet. Since `productFlavors` cannot start with the word 'test' we recommend:

build.gradle:
```groovy
flavorDimensions 'network'
productFlavors {
    // would rather name them "testnet" and "mainnet" but product flavor names cannot start with the word "test"
    zcashtestnet {
        dimension 'network'
        matchingFallbacks = ['zcashtestnet', 'debug']
    }
    zcashmainnet {
        dimension 'network'
        matchingFallbacks = ['zcashmainnet', 'release']
    }
}
```

build.gradle.kts
```kotlin
flavorDimensions.add("network")

productFlavors {
    // would rather name them "testnet" and "mainnet" but product flavor names cannot start with the word "test"
    create("zcashtestnet") {
        dimension = "network"
        matchingFallbacks.addAll(listOf("zcashtestnet", "debug"))
    }

    create("zcashmainnet") {
        dimension = "network"
        matchingFallbacks.addAll(listOf("zcashmainnet", "release"))
    }
}
```

Add the SDK dependency:

```kotlin
implementation("cash.z.ecc.android:zcash-android-sdk:1.4.0-beta01")
```

Start the [Synchronizer](docs/-synchronizer/README.md)

```kotlin
synchronizer.start(this)
```

Get the wallet's address

```kotlin
synchronizer.getAddress()

// or alternatively

DerivationTool.deriveShieldedAddress(viewingKey)
```

Send funds to another address

```kotlin
synchronizer.sendToAddress(spendingKey, zatoshi, address, memo)
```

[Back to contents](#contents)

## Examples

Full working examples can be found in the [demo app](demo-app), covering all major functionality of the SDK. Each demo strives to be self-contained so that a developer can understand everything required for it to work. Testnet builds of the demo app will soon be available to [download as github releases](https://github.com/zcash/zcash-android-wallet-sdk/releases).

### Demos

Menu Item|Related Code|Description
:-----|:-----|:-----
Get Private Key|[GetPrivateKeyFragment.kt](demo-app/src/main/java/cash/z/ecc/android/sdk/demoapp/demos/getprivatekey/GetPrivateKeyFragment.kt)|Given a seed, display its viewing key and spending key
Get Address|[GetAddressFragment.kt](demo-app/src/main/java/cash/z/ecc/android/sdk/demoapp/demos/getaddress/GetAddressFragment.kt)|Given a seed, display its z-addr
Get Balance|[GetBalanceFragment.kt](demo-app/src/main/java/cash/z/ecc/android/sdk/demoapp/demos/getbalance/GetBalanceFragment.kt)|Display the balance
Get Latest Height|[GetLatestHeightFragment.kt](demo-app/src/main/java/cash/z/ecc/android/sdk/demoapp/demos/getlatestheight/GetLatestHeightFragment.kt)|Given a lightwalletd server, retrieve the latest block height
Get Block|[GetBlockFragment.kt](demo-app/src/main/java/cash/z/ecc/android/sdk/demoapp/demos/getblock/GetBlockFragment.kt)|Given a lightwalletd server, retrieve a compact block
Get Block Range|[GetBlockRangeFragment.kt](demo-app/src/main/java/cash/z/ecc/android/sdk/demoapp/demos/getblockrange/GetBlockRangeFragment.kt)|Given a lightwalletd server, retrieve a range of compact blocks
List Transactions|[ListTransactionsFragment.kt](demo-app/src/main/java/cash/z/ecc/android/sdk/demoapp/demos/listtransactions/ListTransactionsFragment.kt)|Given a seed, list all related shielded transactions
Send|[SendFragment.kt](demo-app/src/main/java/cash/z/ecc/android/sdk/demoapp/demos/send/SendFragment.kt)|Send and monitor a transaction, the most complex demo


[Back to contents](#contents)

## Compiling Sources

:warning: Compilation is not required unless you plan to submit a patch or fork the code. Instead, it is recommended to simply add the SDK dependencies via Gradle.

In the event that you *do* want to compile the SDK from sources, please see [Setup.md](docs/Setup.md).

[Back to contents](#contents)

## Versioning

This project follows [semantic versioning](https://semver.org/) with pre-release versions. An example of a valid version number is `1.0.4-alpha11` denoting the `11th` iteration of the `alpha` pre-release of version `1.0.4`. Stable releases, such as `1.0.4` will not contain any pre-release identifiers. Pre-releases include the following, in order of stability: `alpha`, `beta`, `rc`. Version codes offer a numeric representation of the build name that always increases. The first six significant digits represent the major, minor and patch number (two digits each) and the last 3 significant digits represent the pre-release identifier. The first digit of the identifier signals the build type. Lastly, each new build has a higher version code than all previous builds. The following table breaks this down:

#### Build Types

| Type  | Purpose | Stability | Audience | Identifier | Example Version |
| :---- | :--------- | :---------- | :-------- | :------- | :--- |
| **alpha** | **Sandbox.** For developers to verify behavior and try features. Things seen here might never go to production. Most bugs here can be ignored.| Unstable: Expect bugs | Internal developers | 0XX | 1.2.3-alpha04 (10203004) |
| **beta** | **Hand-off.** For developers to present finished features. Bugs found here should be reported and immediately addressed, if they relate to recent changes. | Unstable: Report bugs | Internal stakeholders | 2XX | 1.2.3-beta04 (10203204) |
| **release candidate** | **Hardening.** Final testing for an app release that we believe is ready to go live. The focus here is regression testing to ensure that new changes have not introduced instability in areas that were previously working.  | Stable: Hunt for bugs | External testers | 4XX | 1.2.3-rc04 (10203404) |
| **production** | **Delivery.** Deliver new features to end-users. Any bugs found here need to be prioritized. Some will require immediate attention but most can be worked into a future release. | Stable: Prioritize bugs | Public | 8XX | 1.2.3 (10203800) |

[Back to contents](#contents)

## Examples

A primitive example to exercise the SDK exists in this repo, under [Demo App](demo-app).

There's also a more comprehensive [Sample Wallet](https://github.com/zcash/zcash-android-wallet).

[Back to contents](#contents)

## Checkpoints
To improve the speed of syncing with the Zcash network, the SDK contains a series of embedded checkpoints.  These should be updated periodically, as new transactions are added to the network.  Checkpoints are stored under the [assets](sdk-lib/src/main/assets/co.electriccoin.zcash/checkpoint) directory as JSON files.  Checkpoints for both mainnet and testnet are bundled into the SDK.

To update the checkpoints, see [Checkmate](https://github.com/zcash-hackworks/checkmate).

We generally recommend adding new checkpoints every few weeks.  By convention, checkpoints are added in block increments of 10,000 which provides a reasonable tradeoff in terms of number of checkpoints versus performance.

There are two special checkpoints, one for sapling activation and another for orchard activation.  These are mentioned because they don't follow the "round 10,000" rule.
 * Sapling activation
     * Mainnet: 419200
     * Testnet: 280000
 * Orchard activation
     * Mainnet: 1687104
     * Testnet: 1842420

## Publishing

Publishing instructions for maintainers of this repository can be found in [PUBLISHING.md](PUBLISHING.md)

[Back to contents](#contents)

# Known Issues
1. Intel-based machines may have trouble building in Android Studio.  The workaround is to add the following line to `~/.gradle/gradle.properties` `ZCASH_IS_DEPENDENCY_LOCKING_ENABLED=false`
1. During builds, a warning will be printed that says "Unable to detect AGP versions for included builds. All projects in the build should use the same AGP version."  This can be safely ignored.  The version under build-conventions is the same as the version used elsewhere in the application.
1. Android Studio will warn about the Gradle checksum.  This is a [known issue](https://github.com/gradle/gradle/issues/9361) and can be safely ignored.
