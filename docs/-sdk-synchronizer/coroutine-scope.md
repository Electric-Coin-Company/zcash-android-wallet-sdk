[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk](../index.md) / [SdkSynchronizer](index.md) / [coroutineScope](./coroutine-scope.md)

# coroutineScope

`lateinit var coroutineScope: CoroutineScope`

The lifespan of this Synchronizer. This scope is initialized once the Synchronizer starts
because it will be a child of the parentScope that gets passed into the [start](start.md) function.
Everything launched by this Synchronizer will be cancelled once the Synchronizer or its
parentScope stops. This is a lateinit rather than nullable property so that it fails early
rather than silently, whenever the scope is used before the Synchronizer has been started.

