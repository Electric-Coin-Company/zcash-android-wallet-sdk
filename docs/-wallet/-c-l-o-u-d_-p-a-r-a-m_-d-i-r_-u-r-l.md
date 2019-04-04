[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.secure](../index.md) / [Wallet](index.md) / [CLOUD_PARAM_DIR_URL](./-c-l-o-u-d_-p-a-r-a-m_-d-i-r_-u-r-l.md)

# CLOUD_PARAM_DIR_URL

`const val CLOUD_PARAM_DIR_URL: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

The Url that is used by default in zcashd.
We'll want to make this externally configurable, rather than baking it into the SDK but this will do for now,
since we're using a cloudfront URL that already redirects.

