# Android demo app 

## Introduction
This is a demo app that exercises code in https://github.com/zcash/zcash-android-wallet-sdk, which has all the Android-related functionalities necessary to build a mobile Zcash shielded wallet. 

It relies on [Lightwalletd](https://github.com/zcash/lightwalletd), a backend service that provides a bandwidth-efficient interface to the Zcash blockchain. There is an equivalent [iOS demo app](https://github.com/zcash/ZcashLightClientKit). 

## Requirements
The demo app is built in Kotlin, and targets API 21. The demo directly links the SDK source code so building it also builds the SDK and therefore requires Rust and the NDK. 

## Installation
Refer to [build instructions](https://github.com/zcash/zcash-android-wallet-sdk#compiling-sources) in the readme of the android-wallet-sdk repository for detailed instructions. In short, you will need to: 

1. Install rust: https://www.rust-lang.org/learn/get-started  
2. Clone this repo, https://github.com/zcash/zcash-android-wallet-sdk 
3. Launch from Android Studio, https://developer.android.com/studio  

## Exploring the demo app
After building the app, the emulator should launch. The demo basic app that exercises the related code (see below picture). 

![The android demo app, running in Android Studio](assets/demo-app.png?raw=true "Demo App with Android Studio")

The demo app is not trying to show what's possible, but to present how to accomplish the building blocks of wallet functionality in a simple way in code. 

To explore the app, click on each menu item in order and look at the associated code: 
1. Click `Get Private Key	` to see the private key associated with the address. Look at `GetPrivateKeyFragment.kt` to see how the private key is generated. 
1. Click `Get Address` to see the address. You can send funds to it if you have a wallet that can send to shielded addresses (we recommend ZecWalletLite, if you need one). Look at `GetAddressFragment.kt` to see what to click to copy the address. 
1. Click `Get Latest Height` to see the latest height--it might be at the latest mainnet height if your app has not synced yet. Look at `GetLatestHeightFragment.kt` to see how the app talks to lightwalletd to get the latest block height. 
1. Click `Get Block` to see the 500,000th block. You can see other blocks here, too. Look at  `GetBlockFragment.kt` to see how the blocks are retrieved and processed. 
1. Click `Get Block Range` to see the 500,000th block again. You can request a range of blocks here, so go ahead and try that. Look at `GetBlockRangeFragment.kt` to see how multiple blocks are processed. 
1. Click `List transactions` and wait until all necessary blocks are downloaded. You should see a list of all past incoming transactions. Look at `ListTransactionsFragment.kt` to see how transactions are updated and listed here. Currently, we are not able to retrieve outgoing transactions from scanning the block chain, an app will need to keep track of the transaction information on send (see next step). 
1. Click `Send` and wait for the necessary blocks to download and sync. Even if you had synced from the previous demo, it’s normal behavior to sync again each time. Try sending to the pre-filled address, or to an address of your own. Look at `SendFragment.kt` to see all that is required to send a transaction, as well as handling transaction state. 


## Getting started
We’re assuming you already have a brilliant app idea, a vision for the app’s UI, and know the ins and outs of the Android lifecycle. We’ll just stick to the Zcash app part of “getting started.” 

Similarly, the best way to build a functioning Zcash shielded app is to implement the functionalities that are listed in the demo app, in roughly that order: 

1. Generate and safely store your private key. 
1. Get the associated address, and display it to the user on a receive screen. You may also want to generate a QR code from this address. 
1. Make sure your app can talk to the lightwalletd server and check by asking for the latest height, and verify that it’s current with the Zcash network. 
1. Try interacting with lightwalletd by fetching a block and processing it. Then try fetching a range of blocks, which is much more efficient. 
1. Now that you have the blocks process them and list transactions that send to or are from that wallet, to calculate your balance. 
1. With a current balance (and funds, of course), send a transaction and monitor its transaction status and update the UI with the results. 


## Resources
You don’t need to do it all on your own. 
* Chat with the team who built the kit: [Zcash discord community channel, wallet](https://discord.gg/efFG7UJ)
* Discuss ideas with other community members: [Zcash forum](https://forum.zcashcommunity.com/) 
* Get funded to build a Zcash app: [Zcash foundation grants program](https://grants.zfnd.org/)
* Follow Zcash-specific best practices: [Zcash wallet developer checklist](https://zcash.readthedocs.io/en/latest/rtd_pages/ux_wallet_checklist.html)
* Get more information and see FAQs about the wallet: [Shielded resources documentation](https://zcash.readthedocs.io/en/latest/rtd_pages/shielded_support.html)