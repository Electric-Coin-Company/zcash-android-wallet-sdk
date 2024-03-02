package cash.z.ecc.android.sdk.type

/**
 * Validation helper class, representing result an server endpoint validation
 */
sealed class ServerValidation {
    data object Valid : ServerValidation()

    data object Running : ServerValidation()

    data class InValid(val reason: Throwable) : ServerValidation()
}
