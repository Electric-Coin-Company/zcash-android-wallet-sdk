[zcash-android-wallet-sdk](../../../index.md) / [cash.z.wallet.sdk](../../index.md) / [Synchronizer](../index.md) / [Status](./index.md)

# Status

`enum class Status`

### Enum Values

| Name | Summary |
|---|---|
| [STOPPED](-s-t-o-p-p-e-d.md) | Indicates that [stop](../stop.md) has been called on this Synchronizer and it will no longer be used. |
| [DISCONNECTED](-d-i-s-c-o-n-n-e-c-t-e-d.md) | Indicates that this Synchronizer is disconnected from its lightwalletd server. When set, a UI element may want to turn red. |
| [DOWNLOADING](-d-o-w-n-l-o-a-d-i-n-g.md) | Indicates that this Synchronizer is actively downloading new blocks from the server. |
| [VALIDATING](-v-a-l-i-d-a-t-i-n-g.md) | Indicates that this Synchronizer is actively validating new blocks that were downloaded from the server. Blocks need to be verified before they are scanned. This confirms that each block is chain-sequential, thereby detecting missing blocks and reorgs. |
| [SCANNING](-s-c-a-n-n-i-n-g.md) | Indicates that this Synchronizer is actively decrypting new blocks that were downloaded from the server. |
| [SYNCED](-s-y-n-c-e-d.md) | Indicates that this Synchronizer is fully up to date and ready for all wallet functions. When set, a UI element may want to turn green. In this state, the balance can be trusted. |
