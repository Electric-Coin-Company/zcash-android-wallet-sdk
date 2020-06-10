[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk](../index.md) / [Synchronizer](index.md) / [isValidTransparentAddr](./is-valid-transparent-addr.md)

# isValidTransparentAddr

`abstract suspend fun isValidTransparentAddr(address: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Returns true when the given address is a valid t-addr. Invalid addresses will throw an
exception. Valid t-addresses have these characteristics: //TODO copy info from related ZIP

### Parameters

`address` - the address to validate.

### Exceptions

`RuntimeException` - when the address is invalid.

**Return**
true when the given address is a valid t-addr.

