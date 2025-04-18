package cash.z.ecc.android.sdk.demoapp

import cash.z.ecc.android.sdk.model.AccountCreateSetup
import kotlin.time.Duration.Companion.seconds

// Recommended timeout for Android configuration changes to keep Kotlin Flow from restarting
val ANDROID_STATE_FLOW_TIMEOUT = 5.seconds

/**
 * A tiny weight, useful for spacers to fill an empty space.
 */
const val MINIMAL_WEIGHT = 0.0001f

/**
 * Until we support full multi-account feature in Demo app we use this constant as a single source of truth for
 * account selection
 */
const val CURRENT_ZIP_32_ACCOUNT_INDEX = 0L

/**
 * This value is used across Demo app as a [AccountCreateSetup.keySource] for the default account with index
 * [CURRENT_ZIP_32_ACCOUNT_INDEX]
 */
const val DEMO_APP_ZCASH_ACCOUNT_KEY_SOURCE = "zcash"
