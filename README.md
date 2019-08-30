# Security Disclaimer

#### :warning:  WARNING: This is an *early preview* under active development and *anything may change at anytime!*

----

In the spirit of transparency, we provide this as a window into what we are actively developing. This is an alpha build, not yet intended for 3rd party use. Please be advised of the following:

* 🛑 This code currently is not audited 🛑
* ❌ This is a public, active branch with **no support**
* ❌ The code **does not have** documentation that is reviewed and approved by our Documentation team
* ❌ The code **does not have** adequate unit tests, acceptance tests and stress tests
* ❌ The code **does not have** automated tests that use the officially supported CI system
* ❌ The code **has not been subjected to thorough review** by engineers at the Electric Coin Company
* ❌ This product **does not run** compatibly with the latest version of zcashd
* ❌ The product **is** majorly broken in several ways including:
  * master seed management is left to the 3rd party wallet developer (beacause that's what wallets do best)
  * secure spending key management is left to the 3rd party wallet developer
* ❌ The library **only runs** on testnet
* ❌ The library **does not run** on mainnet and **cannot** run on regtest
* ❌ We **are actively changing** the codebase and adding features where/when needed
* ❌ We **do not** undertake appropriate security coverage (threat models, review, response, etc.)
* :heavy_check_mark: There is a product manager for this library
* :heavy_check_mark: Electric Coin Company maintains the library as we discover bugs and do network upgrades/minor releases
* :heavy_check_mark: Users can expect to get a response within a few weeks after submitting an issue
* ❌ The User Support team **had not yet been briefed** on the features provided to users and the functionality of the associated test-framework
* ❌ The code is **unpolished**
* ❌ The code is **not documented**
* ❌ The code **is not yet published** (to Bintray/Maven Central)
* ❌ Requires external lightwalletd server


 ### 🛑 Use of this code may lead to a loss of funds 🛑 
 
Use of this code in its current form or with modifications may lead to loss of funds, loss of "expected" privacy, or denial of service for a large portion of users, or a bug which could leverage any of those kinds of attacks (especially a "0 day" where we suspect few people know about the vulnerability).

### :eyes: At this time, this is for preview purposes only. :eyes: 

----

# Zcash Android SDK

This lightweight SDK connects Android to Zcash. It welds together Rust and Kotlin in a minimal way, allowing third-party Android apps to send and receive shielded transactions easily, securely and privately.

## Contents

- [Structure](#structure)
- [Overview](#overview)
    - [Components](#components)
- [Quickstart](#quickstart)
- [Compiling Sources](#compiling-sources)
- [Versioning](#versioning)

## Structure

From an app developer's perspective, this SDK will encapsulate the most complex aspects of using Zcash, freeing the developer to focus on UI and UX, rather than scanning blockchains and building commitment trees! Internally, the SDK is structured as follows:


![SDK Diagram](assets/sdk_diagram_final.png?raw=true "SDK Diagram")

Thankfully, the only thing an app developer has to be concerned with is the following:

![SDK Diagram Developer Perspective](assets/sdk_dev_pov_final.png?raw=true "SDK Diagram Dev PoV")  

[Back to contents](#contents)
## Overview

At a high level, this SDK simply helps native Android codebases connect to Zcash's Rust crypto libraries without needing to know Rust or be a Cryptographer. Think of it as welding. The SDK takes separate things and tightly bonds them together such that each can remain as idiomatic as possible. It's goal is to make it easy for an app to incorporate shielded transactions while remaining a good citizen on mobile devices. 

Given all the moving parts, making things easy requires coordination. The [Synchronizer](docs/-synchronizer/README.md) provides that layer of abstraction so that the primary steps to make use of this SDK are simply:

  1. Start the [Synchronizer](docs/-synchronizer/README.md)
  2. Subscribe to wallet data
  
The [Synchronizer](docs/-synchronizer/README.md) takes care of

    - Connecting to the light wallet server
    - Downloading the latest compact blocks in a privacy-sensitive way
    - Scanning and trial decrypting those blocks for shielded transactions related to the wallet
    - Processing those related transactions into useful data for the UI
    - Sending payments to a full node through the light wallet server
    - Monitoring sent payments for status updates

To accomplish this, these responsibilities of the SDK are divided into separate components. Each component is coordinated by the [Synchronizer](docs/-synchronizer/README.md), which is the thread that ties it all together.

#### Components

| Component  | Summary |
| :--------- | :------ |
| **LightWalletService** | Service used for requesting compact blocks |
| **CompactBlockStore** | Stores compact blocks that have been downloaded from the `LightWalletService` |
| **CompactBlockProcessor** | Validates and scans the compact blocks in the `CompactBlockStore` for transaction details |
| **TransactionManager** | Creates, Submits and manages transactions for spending funds |
| **Wallet** | Wraps the Zcash rust libraries, insulating SDK users from changes in that layer |

[Back to contents](#contents)
## Quickstart

Add the SDK dependency
```gradle
implementation "cash.z.android.wallet:zcash-android-testnet:1.0.0-alpha01@aar"
```
Start the [Synchronizer](docs/-synchronizer/README.md)

```kotlin
synchronizer.start(this)
```
Get the wallet's address
```kotlin
synchronizer.getAddress()
```
Send funds to another address
```kotlin
synchronizer.sendToAddress(zatoshi, address, memo)
```

[Back to contents](#contents)
## Compiling Sources

:warning: Presently, the latest stable code lives in the `preview` branch, under active development, and is not yet released. 

Importing the dependency should be enough for use but in the event that you want to compile the SDK from sources, including the Kotlin and Rust portions, simply use Gradle.

Compilation requires `Cargo` and has been tested on Ubuntu, MacOS and Windows. To compile the SDK run:

```bash
./gradlew clean assembleZcashtestnetRelease
```
This creates a `testnet` build of the SDK that can be used to preview basic functionality for sending and receiving shielded transactions. If you do not have `Rust` and `Cargo` installed, the build script will let you know and provide further instructions for installation. Note that merely using the SDK does not require installing Rust or Cargo--that is only required for compilation.


[Back to contents](#contents)
## Versioning

This project follows [semantic versioning](https://semver.org/) with pre-release versions. An example of a valid version number is `1.0.4-alpha11` denoting the `11th` iteration of the `alpha` pre-release of version `1.0.4`. Stable releases, such as `1.0.4` will not contain any pre-release identifiers. Pre-releases include the following, in order of stability: `alpha`, `beta`, `rc`. Version codes offer a numeric representation of the build name that always increases. The first six significant digits represent the major, minor and patch number (two digits each) and the last 3 significant digits represent the pre-release identifier. The first digit of the identifier signals the build type. Lastly, each new build has a higher version code than all previous builds. The following table breaks this down:

#### Build Types
| Type  | Purpose | Stability | Audience | Identifier | Example Version |
| :---- | :--------- | :---------- | :-------- | :------- | :--- |
| **alpha** | **Sandbox.** For developers to verify behavior and try features. Things seen here might never go to production. Most bugs here can be ignored.| Unstable: Expect bugs | Internal developers | 0XX | 1.2.3-alpha04 (10203004) |
| **beta** | **Hand-off.** For developers to present finished features. Bugs found here should be reported and immediately addressed, if they relate to recent changes. | Unstable: Report bugs | Internal stakeholders | 2XX | 1.2.3-beta04 (10203204) |
| **release candidate** | **Hardening.** Final testing for an app release that we believe is ready to go live. The focus here is regression testing to ensure that new changes have not introduced instability in areas that were previously working.  | Stable: Hunt for bugs | External testers | 4XX | 1.2.3-rc04 (10203404) |
| **production** | **Dellivery.** Deliver new features to end users. Any bugs found here need to be prioritized. Some will require immediate attention but most can be worked into a future release. | Stable: Prioritize bugs | Public | 8XX | 1.2.3 (10203800) |

[Back to contents](#contents)
