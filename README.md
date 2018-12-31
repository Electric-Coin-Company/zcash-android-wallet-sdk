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
