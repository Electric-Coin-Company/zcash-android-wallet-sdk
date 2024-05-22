package cash.z.ecc.android.sdk.internal.model

import co.electriccoin.lightwallet.client.BuildConfig

/**
 * List of supported log levels used within the Rust SDK layer
 *
 * WARN: Do not change the string identifiers, as those are agreed by both Kotlin and Rust SDK layers.
 */
sealed class RustLogging(open val identifier: String) {
    /**
     * No logs are printed. This is a required option for the production SDK build.
     */
    data object Off : RustLogging("off")

    /**
     * Logs with lower priority information
     */
    data object Debug : RustLogging("debug")

    /**
     * Logs with very low priority, often extremely verbose
     */
    data object Trace : RustLogging("trace")
}

// WARN: Only change the logic below if you understand and require changes in it
// [RustBackend.rustLogging] value is supposed to be changed to achieve a different Rust logging level in development
fun RustLogging.isNotLoggingInProduction() =
    if (BuildConfig.BUILD_TYPE.contains("release")) {
        this == RustLogging.Off
    } else {
        true
    }
