[![license](https://img.shields.io/github/license/zcash/zcash-android-wallet-sdk.svg?maxAge=2592000&style=plastic)](https://github.com/zcash/zcash-android-wallet-sdk/blob/master/LICENSE)
[![@gmale](https://img.shields.io/badge/contact-android@z.cash-5AA9E7.svg?style=plastic)](https://github.com/gmale)
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
Add the SDK dependency:

```groovy
implementation 'cash.z.ecc.android:zcash-android-sdk:1.3.0-beta10'
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

In the event that you *do* want to compile the SDK from sources, follow these steps:

1. [Install rust](https://www.rust-lang.org/learn/get-started)
   1. If you're a macOS user with homebrew already installed
       1. `brew install rustup`
       1. `rustup-init`
2. Then, add the android targets via:
```bash
rustup target add armv7-linux-androideabi aarch64-linux-android i686-linux-android x86_64-linux-android
```
3. Clone this repo 
4. [Install Android Studio](https://developer.android.com/studio/install) and open this project via `/your/path/to/zcash-android-wallet-sdk/build.gradle.kts`
5. Open Android Studio’s SDK manager
<p align="center">
    <img src="assets/sdk-manager-icon.png?raw=true" width="70%"/>
</p>    

  6. Then, install NDK ~~20.0.5594570~~ 22.1.7171670
     (pro tip: `build.gradle -> ndkVersion` defines the actual required version. Use that because this README may get out-of-date. Also note that the "Show Package Details" box in the bottom right corner must be checked in order to install specific versions of the NDK.)
<p align="center">
    <img src="assets/ndk-window.png?raw=true" width="85%"/>
</p>

  8. To build from the command line, run:
  ```bash
  ./gradlew assemble
  
  // or to install in MavenLocal
  
  ./gradlew publishToMavenLocal
  ```

This creates a build of the SDK under `build/outputs/aar/` that can be used to preview functionality. For more detailed examples, see the [demo app](demo-app).

Note that merely using the SDK does not require installing Rust or Cargo--that is only required when compiling from source. Also note that the Mozilla Rust Gradle plugin puts outputs under `sdk-lib/targets`, which has implications for manually testing build script changes. This is discussed further under [docs/tests/Build.md](docs/tests/Build.md).

The repo also contains a small demo application, to verify integration with the SDK.  Note that by default, the demo application is configured to retrieve dependencies from artifact hosting and therefore does not rely on the local compilation of the SDK.  This can be changed by publishing to maven local as described above, as local maven publications will take precedence over hosted publications in the demo app.
1. [Create an emulator](https://developer.android.com/studio/run/managing-avds) if you don’t already have one (recommended target: API 31)
2. Import the subdirectory samples/demo-app as a separate Android Studio project
3. Select your desired build variant. Currently, we recommend `zcashmainnetDebug` as the testnet variants are slower to sync to current height due to a lack of checkpoints.
<p align="center">
    <img src="assets/build-variants.png?raw=true" width="54%"/>
</p>

4. Sync project with Gradle files, and build from the IDE. Alternatively, to build from the command line run:
  ```bash
  ./gradlew clean assembleZcashmainnetDebug
  ```

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

## Publishing

Publishing instructions for maintainers of this repository can be found in [PUBLISHING.md](PUBLISHING.md)

[Back to contents](#contents)

# Known Issues

1. During builds, a warning will be printed that says "Unable to detect AGP versions for included builds. All projects in the build should use the same AGP version."  This can be safely ignored.  The version under build-conventions is the same as the version used elsewhere in the application.
1. Android Studio will warn about the Gradle checksum.  This is a [known issue](https://github.com/gradle/gradle/issues/9361) and can be safely ignored.
1. `ktlintFormat` may fail on Java 16 or greater.  A temporary workaround is to install an older JDK and explicitly set the JDK version when executing the task.  For example: `./gradlew ktlintFormat -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home`
1. Some JDK distributions don't offer older versions for M1 Macs.  [Azul Zulu](https://www.azul.com/downloads/?os=macos&architecture=arm-64-bit&package=jdk) does offer JDK 11 and JDK 15 for M1 Macs, which should be compatible with our builds.