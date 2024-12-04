package cash.z.ecc.android.sdk.demoapp

import kotlin.time.Duration.Companion.seconds

// Recommended timeout for Android configuration changes to keep Kotlin Flow from restarting
val ANDROID_STATE_FLOW_TIMEOUT = 5.seconds

/**
 * A tiny weight, useful for spacers to fill an empty space.
 */
const val MINIMAL_WEIGHT = 0.0001f

// TODO [#1644]: Refactor Account ZIP32 index across SDK
// TODO [#1644]: https://github.com/Electric-Coin-Company/zcash-android-wallet-sdk/issues/1644
const val CURRENT_ZIP_32_ACCOUNT_INDEX = 0
