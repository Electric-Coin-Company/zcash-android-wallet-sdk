```diff
-🛑 WARNING: *Early preview* 🛑  
```
In the spirit of transparency, we provide this as a window into what we are actively developing. This is an alpha build, not yet intended for 3rd party use. Please be advised of the following:

* 🛑 This code currently is not audited 🛑
* ❌ This is a public, active branch with **no support**
* ❌ The code **does not have** documentation that is reviewed and approved by our Documentation team
* ❌ The code **does not have** unit tests, acceptance tests and stress tests
* ❌ The code **does not have** automated tests that use the officially supported CI system
* ❌ All merged code **has not received** a code review by people at Zcash Company
* ❌ This product **does not run** compatibly with the latest version of zcashd
* ❌ The product **is** majorly broken in several ways
* ❌ The library **only runs** on testnet
* ❌ The library **does not run** on mainnet and **cannot** run on regtest
* ❌ We **are actively changing** the codebase and adding features where/when needed
* ❌ We **do not** undertake appropriate security coverage (threat models, review, response, etc.)
* :heavy_check_mark: There is a product manager for this library
* :heavy_check_mark: Zcash Company maintains the library as we discover bugs and do network upgrades/minor releases
* :heavy_check_mark: Users can expect to get a response within a few weeks after submitting an issue
* ❌ The User Support team **had not yet been briefed** on the features provided to users and the functionality of the associated test-framework

 ### 🛑 Use of this code may lead to a loss of funds 🛑 
 
 Use of this code my alead to loss of funds, loss of "expected" privacy, or denial of service for a large portion of users, or a bug which could leverage any of those kinds of attacks (especially a "0 day" where we suspect few people know about the vulnerability).

### :eyes: At this time, this is for preview purposes only. :eyes: 


# Zcash Android SDK

This lightweight SDK connects Android to Zcash. It welds together Rust and Kotlin in a minimal way, allowing third-party Android apps to send and receive shielded transactions easily, securely and privately.

# Structure

From an app developer's perspective, this SDK will encapsulate the most complex aspects of using Zcash, freeing the developer to focus on UI and UX, rather than scanning blockchains and building commitment trees! Internally, the SDK is structured as follows:


![SDK Diagram](assets/sdk-diagram.png?raw=true "SDK Diagram DRAFT")

Thankfully, the only thing an app developer has to be concerned with is the following:

![SDK Diagram Developer Perspective](assets/sdk_dev_pov.png?raw=true "SDK Diagram Dev POV DRAFT")

The primary steps for a 3rd party developer to make use of this SDK are simply:

  1. Start the synchronizer
  2. Consume transactions from the repository
  
The Sychronizer takes care of
  
    - Connecting to the light wallet server
    - Downloading the latest blocks in a privacy-sensitive way
    - Scanning those blocks for shielded transactions related to the wallet
    - Processing those related transactions into useful model data for the UI
    
The data produced by the synchronizer is exposed via a repository object. The repository provides `ReceiveChannels` that broadcast transaction and balance information. This allows the wallet to simply subscribe to those channels and stay updated with the latest shielded transaction information.
