package cash.z.ecc.android.sdk.model

/**
 * A unit of currency used throughout the SDK.
 *
 * End users (e.g. app users) generally are not shown Zatoshi values.  Instead they are presented
 * with ZEC, which is a decimal value represented only as a String.  ZEC are not used internally,
 * to avoid floating point imprecision.
 */
data class Zatoshi(val value: Long) {
    init {
        require(value >= MIN_INCLUSIVE) { "Zatoshi must be in the range [0, $MAX_INCLUSIVE]" }
        require(value <= MAX_INCLUSIVE) { "Zatoshi must be in the range [0, $MAX_INCLUSIVE]" }
    }

    operator fun plus(other: Zatoshi) = Zatoshi(value + other.value)
    operator fun minus(other: Zatoshi) = Zatoshi(value - other.value)

    companion object {
        /**
         * The number of Zatoshi that equal 1 ZEC.
         */
        const val ZATOSHI_PER_ZEC = 100_000_000L

        private const val MAX_ZEC_SUPPLY = 21_000_000

        const val MIN_INCLUSIVE = 0

        const val MAX_INCLUSIVE = ZATOSHI_PER_ZEC * MAX_ZEC_SUPPLY
    }
}
