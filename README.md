# Security Disclaimer

#### :warning:  WARNING: This is an *early preview*

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
* :heavy_check_mark: Zcash Company maintains the library as we discover bugs and do network upgrades/minor releases
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

# Usage

:warning: Presently, the latest stable code lives in the `preview` branch, under active development, and is not yet released. 

Compilation requires `Cargo` and has been tested on Ubuntu, MacOS and Windows. To compile the SDK run:

```bash
./gradlew assembleZcashtestnetRelease
```
This creates a `testnet` build of the SDK that can be used to preview basic functionality for sending and receiving shielded transactions. If you do not have `Rust` and `Cargo` installed, the build script will let you know and provide further instructions for installation.

# Structure

From an app developer's perspective, this SDK will encapsulate the most complex aspects of using Zcash, freeing the developer to focus on UI and UX, rather than scanning blockchains and building commitment trees! Internally, the SDK is structured as follows:


![SDK Diagram](assets/sdk-diagram.png?raw=true "SDK Diagram DRAFT")

Thankfully, the only thing an app developer has to be concerned with is the following:

![SDK Diagram Developer Perspective](assets/sdk_dev_pov.png?raw=true "SDK Diagram Dev POV DRAFT")

The primary steps for a 3rd party developer to make use of this SDK are simply:

  1. Start the synchronizer
  2. Consume wallet data via channels 
  
The Sychronizer takes care of

    - Connecting to the light wallet server
    - Downloading the latest compact blocks in a privacy-sensitive way
    - Scanning and trial decrypting those blocks for shielded transactions related to the wallet
    - Processing those related transactions into useful data for the UI
    - Sending payments to a full node through the light wallet server
    - Monitoring sent payments for status updates

At a high level, the Synchronizer provides ReceiveChannels that broadcast transaction and balance information. This allows the wallet to simply subscribe to those channels and stay updated with the latest shielded transaction information.
    
At a more granular level...

Visit the [preview branch](https://github.com/zcash/zcash-android-wallet-sdk/tree/preview) for more detailed documentation and the latest code.

:warning: Presently, the latest stable code lives in the `preview` branch, under active development, and is not yet released.
