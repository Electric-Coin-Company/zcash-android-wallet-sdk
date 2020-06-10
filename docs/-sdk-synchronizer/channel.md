[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk](../index.md) / [SdkSynchronizer](index.md) / [channel](./channel.md)

# channel

`val channel: ManagedChannel`

The channel that this Synchronizer uses to communicate with lightwalletd. In most cases, this
should not be needed or used. Instead, APIs should be added to the synchronizer to
enable the desired behavior. In the rare case, such as testing, it can be helpful to share
the underlying channel to connect to the same service, and use other APIs
(such as darksidewalletd) because channels are heavyweight.

