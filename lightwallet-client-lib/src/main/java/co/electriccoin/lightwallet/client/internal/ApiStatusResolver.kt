package co.electriccoin.lightwallet.client.internal

import co.electriccoin.lightwallet.client.model.Response

/**
 * This class provides conversion from API statuses to our predefined Server or Client error classes.
 */
interface ApiStatusResolver {
    fun <T> resolveFailureFromStatus(throwable: Throwable): Response.Failure<T>
}
