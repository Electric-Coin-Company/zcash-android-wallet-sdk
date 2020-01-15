[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk](../index.md) / [SdkSynchronizer](index.md) / [isValidTransparentAddr](./is-valid-transparent-addr.md)

# isValidTransparentAddr

`suspend fun isValidTransparentAddr(address: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Overrides [Synchronizer.isValidTransparentAddr](../-synchronizer/is-valid-transparent-addr.md)

Returns true when the given address is a valid t-addr. Invalid addresses will throw an
exception. Valid t-addresses have these characteristics: //TODO

### Parameters

`address` - the address to validate.

### Exceptions

`RuntimeException` - when the address is invalid.