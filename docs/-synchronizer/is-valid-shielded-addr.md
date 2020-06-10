[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk](../index.md) / [Synchronizer](index.md) / [isValidShieldedAddr](./is-valid-shielded-addr.md)

# isValidShieldedAddr

`abstract suspend fun isValidShieldedAddr(address: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Returns true when the given address is a valid z-addr. Invalid addresses will throw an
exception. Valid z-addresses have these characteristics: //TODO copy info from related ZIP

### Parameters

`address` - the address to validate.

### Exceptions

`RuntimeException` - when the address is invalid.

**Return**
true when the given address is a valid z-addr.

