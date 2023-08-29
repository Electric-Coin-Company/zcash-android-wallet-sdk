package cash.z.ecc.android.sdk.internal.ext

import cash.z.ecc.android.sdk.ext.ZcashSdk.MAX_BACKOFF_INTERVAL
import cash.z.ecc.android.sdk.internal.Twig
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.random.Random

/**
 * Execute the given block and if it fails, retry up to [retries] more times. If none of the
 * retries succeed then throw the final error, which can be wrapped in order to add more context.
 *
 * @param retries the number of times to retry the block after the first attempt fails.
 * @param exceptionWrapper a function that can wrap the final failure to add more useful information
 * or context. Default behavior is to just return the final exception.
 * @param initialDelayMillis the initial amount of time to wait before the first retry.
 * @param block the code to execute, which will be wrapped in a try/catch and retried whenever an
 * exception is thrown up to [retries] attempts.
 */
suspend inline fun retryUpToAndThrow(
    retries: Int,
    exceptionWrapper: (Throwable) -> Throwable = { it },
    initialDelayMillis: Long = 500L,
    block: (Int) -> Unit
) {
    var failedAttempts = 0
    while (failedAttempts <= retries) {
        @Suppress("TooGenericExceptionCaught")
        try {
            block(failedAttempts)
            return
        } catch (t: Throwable) {
            failedAttempts++
            if (failedAttempts > retries) {
                throw exceptionWrapper(t)
            }
            val duration = (initialDelayMillis.toDouble() * 2.0.pow(failedAttempts.toDouble() - 1)).toLong()
            Twig.warn(t) { "Retrying ($failedAttempts/$retries) in ${duration}s..." }
            delay(duration)
        }
    }
}

/**
 * Execute the given block and if it fails, retry up to [retries] more times. If none of the
 * retries succeed, then leave the block execution unfinished and continue.
 *
 * @param retries the number of times to retry the block after the first attempt fails.
 * @param exceptionWrapper a function that can wrap the final failure to add more useful information
 *  * or context. Default behavior is to just return the final exception.
 * @param initialDelayMillis the initial amount of time to wait before the first retry.
 * @param block the code to execute, which will be wrapped in a try/catch and retried whenever an
 * exception is thrown up to [retries] attempts.
 */
suspend inline fun retryUpToAndContinue(
    retries: Int,
    exceptionWrapper: (Throwable) -> Throwable = { it },
    initialDelayMillis: Long = 500L,
    block: (Int) -> Unit
) {
    var failedAttempts = 0
    while (failedAttempts < retries) {
        @Suppress("TooGenericExceptionCaught")
        try {
            block(failedAttempts)
            return
        } catch (t: Throwable) {
            failedAttempts++
            if (failedAttempts > retries) {
                exceptionWrapper(t)
            }
            val duration = (initialDelayMillis.toDouble() * 2.0.pow(failedAttempts.toDouble() - 1)).toLong()
            Twig.warn(t) { "Retrying ($failedAttempts/$retries) in ${duration}s..." }
            delay(duration)
        }
    }
}

/**
 * Execute the given block and if it fails, retry with an exponential backoff.
 *
 * @param onErrorListener a callback that gets the first shot at processing any error and can veto
 * the retry behavior by returning false.
 * @param initialDelayMillis the initial delay before retrying.
 * @param maxDelayMillis the maximum delay between retries.
 * @param block the logic to run once and then run again if it fails.
 */
@Suppress("MagicNumber")
suspend inline fun retryWithBackoff(
    noinline onErrorListener: ((Throwable) -> Boolean)? = null,
    initialDelayMillis: Long = 1000L,
    maxDelayMillis: Long = MAX_BACKOFF_INTERVAL,
    block: () -> Unit
) {
    // count up to the max and then reset to half. So that we don't repeat the max but we also don't repeat too much.
    var sequence = 0
    while (true) {
        @Suppress("TooGenericExceptionCaught")
        try {
            block()
            return
        } catch (t: Throwable) {
            // offer to listener first
            if (onErrorListener?.invoke(t) == false) {
                throw t
            }

            sequence++
            // initialDelay^(sequence/4) + jitter
            var duration = initialDelayMillis.toDouble().pow((sequence.toDouble() / 4.0)).toLong() +
                Random.nextLong(1000L)
            if (duration > maxDelayMillis) {
                duration = maxDelayMillis - Random.nextLong(1000L) // include jitter but don't exceed max delay
                sequence /= 2
            }
            Twig.warn(t) { "backing off and retrying in ${duration}ms..." }
            delay(duration)
        }
    }
}
