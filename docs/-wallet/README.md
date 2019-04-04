[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.secure](../index.md) / [Wallet](./index.md)

# Wallet

`class Wallet`

Wrapper for the converter. This class basically represents all the Rust-wallet capabilities and the supporting data
required to exercise those abilities.

### Parameters

`birthday` - the birthday of this wallet. See [WalletBirthday](-wallet-birthday/index.md) for more info.

### Types

| Name | Summary |
|---|---|
| [WalletBalance](-wallet-balance/index.md) | `data class WalletBalance`<br>Data structure to hold the total and available balance of the wallet. This is what is received on the balance channel. |
| [WalletBirthday](-wallet-birthday/index.md) | `data class WalletBirthday`<br>Represents the wallet's birthday which can be thought of as a checkpoint at the earliest moment in history where transactions related to this wallet could exist. Ideally, this would correspond to the latest block height at the time the wallet key was created. Worst case, the height of Sapling activation could be used (280000). |

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `Wallet(context: Context, converter: `[`JniConverter`](../../cash.z.wallet.sdk.jni/-jni-converter/index.md)`, dataDbPath: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, paramDestinationDir: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, accountIds: `[`Array`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)`<`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`> = arrayOf(0), seedProvider: `[`ReadOnlyProperty`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.properties/-read-only-property/index.html)`<`[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?, `[`ByteArray`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)`>, spendingKeyProvider: `[`ReadWriteProperty`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.properties/-read-write-property/index.html)`<`[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?, `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>)``Wallet(birthday: `[`Wallet.WalletBirthday`](-wallet-birthday/index.md)`, converter: `[`JniConverter`](../../cash.z.wallet.sdk.jni/-jni-converter/index.md)`, dataDbPath: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, paramDestinationDir: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, accountIds: `[`Array`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)`<`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`> = arrayOf(0), seedProvider: `[`ReadOnlyProperty`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.properties/-read-only-property/index.html)`<`[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?, `[`ByteArray`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)`>, spendingKeyProvider: `[`ReadWriteProperty`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.properties/-read-write-property/index.html)`<`[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?, `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>)`<br>Wrapper for the converter. This class basically represents all the Rust-wallet capabilities and the supporting data required to exercise those abilities. |

### Functions

| Name | Summary |
|---|---|
| [balance](balance.md) | `fun balance(): ReceiveChannel<`[`Wallet.WalletBalance`](-wallet-balance/index.md)`>`<br>Stream of balances. |
| [createRawSendTransaction](create-raw-send-transaction.md) | `suspend fun createRawSendTransaction(value: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, toAddress: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, memo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = "", fromAccountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = accountIds[0]): `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)<br>Does the proofs and processing required to create a raw transaction and inserts the result in the database. On average, this call takes over 10 seconds. |
| [fetchParams](fetch-params.md) | `suspend fun fetchParams(destinationDir: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Download and store the params into the given directory. |
| [getAddress](get-address.md) | `fun getAddress(accountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = accountIds[0]): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Gets the address for this wallet, defaulting to the first account. |
| [initialize](initialize.md) | `fun initialize(firstRunStartHeight: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = SAPLING_ACTIVATION_HEIGHT): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Initializes the wallet by creating the DataDb and pre-populating it with data corresponding to the birthday for this wallet. |
| [sendBalanceInfo](send-balance-info.md) | `suspend fun sendBalanceInfo(accountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = accountIds[0]): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Calculates the latest balance info and emits it into the balance channel. Defaults to the first account. |

### Companion Object Properties

| Name | Summary |
|---|---|
| [BIRTHDAY_DIRECTORY](-b-i-r-t-h-d-a-y_-d-i-r-e-c-t-o-r-y.md) | `const val BIRTHDAY_DIRECTORY: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Directory within the assets folder where birthday data (i.e. sapling trees for a given height) can be found. |
| [CLOUD_PARAM_DIR_URL](-c-l-o-u-d_-p-a-r-a-m_-d-i-r_-u-r-l.md) | `const val CLOUD_PARAM_DIR_URL: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>The Url that is used by default in zcashd. We'll want to make this externally configurable, rather than baking it into the SDK but this will do for now, since we're using a cloudfront URL that already redirects. |
| [OUTPUT_PARAM_FILE_NAME](-o-u-t-p-u-t_-p-a-r-a-m_-f-i-l-e_-n-a-m-e.md) | `const val OUTPUT_PARAM_FILE_NAME: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>File name for the sapling output params |
| [SPEND_PARAM_FILE_NAME](-s-p-e-n-d_-p-a-r-a-m_-f-i-l-e_-n-a-m-e.md) | `const val SPEND_PARAM_FILE_NAME: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>File name for the sappling spend params |

### Companion Object Functions

| Name | Summary |
|---|---|
| [loadBirthdayFromAssets](load-birthday-from-assets.md) | `fun loadBirthdayFromAssets(context: Context, birthdayHeight: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`? = null): `[`Wallet.WalletBirthday`](-wallet-birthday/index.md)<br>Load the given birthday file from the assets of the given context. When no height is specified, we default to the file with the greatest name. |
