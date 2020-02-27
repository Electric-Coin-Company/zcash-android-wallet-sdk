package cash.z.wallet.sdk.ext

import android.content.Context
import cash.z.wallet.sdk.ext.ZcashSdk.MAX_BACKOFF_INTERVAL
import kotlinx.coroutines.delay
import java.io.File
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
suspend inline fun retryUpTo(retries: Int, exceptionWrapper: (Throwable) -> Throwable = { it }, initialDelayMillis: Long = 500L, block: (Int) -> Unit) {
    var failedAttempts = 0
    while (failedAttempts <= retries) {
        try {
            block(failedAttempts)
            return
        } catch (t: Throwable) {
            failedAttempts++
            if (failedAttempts > retries) throw exceptionWrapper(t)
            val duration = (initialDelayMillis.toDouble() * Math.pow(2.0, failedAttempts.toDouble() - 1)).toLong()
            twig("failed due to $t retrying (${failedAttempts}/$retries) in ${duration}s...")
            delay(duration)
        }
    }
}

/**
 * Execute the given block and if it fails, retry up to [retries] more times, using thread sleep
 * instead of suspending. If none of the retries succeed then throw the final error. This function
 * is intended to be called with no parameters, i.e., it is designed to use its defaults.
 *
 * @param retries the number of times to retry. Typically, this should be low.
 * @param sleepTime the amount of time to sleep in between retries. Typically, this should be an
 * amount of time that is hard to perceive.
 * @param block the block of logic to try.
 */
inline fun retrySimple(retries: Int = 2, sleepTime: Long = 20L, block: (Int) -> Unit) {
    var failedAttempts = 0
    while (failedAttempts <= retries) {
        try {
            block(failedAttempts)
            return
        } catch (t: Throwable) {
            failedAttempts++
            if (failedAttempts > retries) throw t
            twig("failed due to $t simply retrying (${failedAttempts}/$retries) in ${sleepTime}ms...")
            Thread.sleep(sleepTime)
        }
    }
}

/**
 * Execute the given block and if it fails, retry with an exponential backoff.
 *
 * @param onErrorListener a callback that gets the first shot at processing any error and can veto
 * the retry behavior by returning false.
 * @param initialDelayMillis the initial delay before retrying.
 * @param maxDelayMillis the maximum delay between retrys.
 * @param block the logic to run once and then run again if it fails.
 */
suspend inline fun retryWithBackoff(noinline onErrorListener: ((Throwable) -> Boolean)? = null, initialDelayMillis: Long = 1000L, maxDelayMillis: Long = MAX_BACKOFF_INTERVAL, block: () -> Unit) {
    var sequence = 0 // count up to the max and then reset to half. So that we don't repeat the max but we also don't repeat too much.
    while (true) {
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
            var duration = Math.pow(initialDelayMillis.toDouble(), (sequence.toDouble()/4.0)).toLong() + Random.nextLong(1000L)
            if (duration > maxDelayMillis) {
                duration = maxDelayMillis - Random.nextLong(1000L) // include jitter but don't exceed max delay
                sequence /= 2
            }
            twig("Failed due to $t backing off and retrying in ${duration}ms...")
            delay(duration)
        }
    }
}

/**
 * Return true if the given database already exists.
 *
 * @return true when the given database exists in the given context.
 */
internal fun dbExists(appContext: Context, dbFileName: String): Boolean {
    return File(appContext.getDatabasePath(dbFileName).absolutePath).exists()
}