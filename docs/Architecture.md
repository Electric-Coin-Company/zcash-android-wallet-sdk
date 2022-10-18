# Overview
From an app developer's perspective, this SDK will encapsulate the most complex aspects of using Zcash, freeing the developer to focus on UI and UX, rather than scanning blockchains and building commitment trees! Internally, the SDK is structured as follows:

![SDK Diagram](assets/sdk_diagram_final.png?raw=true "SDK Diagram")

Thankfully, the only thing an app developer has to be concerned with is the following:

![SDK Diagram Developer Perspective](assets/sdk_dev_pov_final.png?raw=true "SDK Diagram Dev PoV")

# Components

| Component                              | Summary                                                                                                                             |
| -------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| **LightWalletService**                 | Service used for requesting compact blocks                                                                                          |
| **CompactBlockStore**                  | Stores compact blocks that have been downloaded from the `LightWalletService`                                                       |
| **CompactBlockProcessor**              | Validates and scans the compact blocks in the `CompactBlockStore` for transaction details                                           |
| **OutboundTransactionManager**         | Creates, Submits and manages transactions for spending funds                                                                        |
| **DerivationTool**                     | Utilities for deriving keys and addresses                                                                                           |
| **RustBackend**                        | Wraps and simplifies the rust library and exposes its functionality to the Kotlin SDK                                               |

# Checkpoints
To improve the speed of syncing with the Zcash network, the SDK contains a series of embedded checkpoints.  These should be updated periodically, as new transactions are added to the network.  Checkpoints are stored under the [sdk-lib's assets](../sdk-lib/src/main/assets/co.electriccoin.zcash/checkpoint) directory as JSON files.  Checkpoints for both mainnet and testnet are bundled into the SDK.

To update the checkpoints, see [Checkmate](https://github.com/zcash-hackworks/checkmate).

We generally recommend adding new checkpoints every few weeks.  By convention, checkpoints are added in block 
increments of 2,500 for mainnet and 10,000 for testnet. These increments provide a reasonable tradeoff in terms of number of checkpoints versus performance.

There are two special checkpoints, one for sapling activation and another for orchard activation.  These are 
mentioned because they don't follow the "round 2,500 or 10,000" rule.
 * Sapling activation
     * Mainnet: 419200
     * Testnet: 280000
 * Orchard activation
     * Mainnet: 1687104
     * Testnet: 1842420

Note: If you're updating the checkpoints with the Checkmate tool, it'll generate several checkpoint files from input parameters for you. Mostly you can filter out the first and the last one, as the first is probably already imported in the project and the last one does not follow the round rule.
