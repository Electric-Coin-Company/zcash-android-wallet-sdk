[zcash-android-wallet-sdk](../../../../index.md) / [cash.z.ecc.android.sdk.data](../../../index.md) / [SdkSynchronizer](../../index.md) / [SyncState](../index.md) / [CacheOnly](index.md) / [&lt;init&gt;](./-init-.md)

# &lt;init&gt;

`CacheOnly(startingBlockHeight: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = Int.MAX_VALUE)`

State for when compact blocks have been downloaded but not scanned. This state is typically achieved when the
app was previously started but killed before the first scan took place. In this case, we do not need to
download compact blocks that we already have.

### Parameters

`startingBlockHeight` - the last block that has been downloaded into the cache. We do not need to download
any blocks before this height because we already have them.
