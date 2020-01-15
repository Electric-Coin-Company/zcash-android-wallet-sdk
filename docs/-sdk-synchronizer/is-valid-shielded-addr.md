[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk](../index.md) / [SdkSynchronizer](index.md) / [isValidShieldedAddr](./is-valid-shielded-addr.md)

# isValidShieldedAddr

`suspend fun isValidShieldedAddr(address: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Overrides [Synchronizer.isValidShieldedAddr](../-synchronizer/is-valid-shielded-addr.md)

Returns true when the given address is a valid z-addr. Invalid addresses will throw an
exception. Valid z-addresses have these characteristics: //TODO

### Parameters

`address` - the address to validate.

### Exceptions

`RuntimeException` - when the address is invalid.