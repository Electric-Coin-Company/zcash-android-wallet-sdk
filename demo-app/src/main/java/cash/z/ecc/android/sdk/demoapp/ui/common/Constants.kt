package cash.z.ecc.android.sdk.demoapp.ui.common

import kotlin.time.Duration.Companion.seconds

// Recommended timeout for Android configuration changes to keep Kotlin Flow from restarting
val ANDROID_STATE_FLOW_TIMEOUT = 5.seconds

/**
 * A tiny weight, useful for spacers to fill an empty space.
 */
const val MINIMAL_WEIGHT = 0.0001f
