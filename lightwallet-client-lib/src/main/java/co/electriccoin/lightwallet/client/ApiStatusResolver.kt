package co.electriccoin.lightwallet.client

import co.electriccoin.lightwallet.client.model.Response

/**
 * This class provides conversion from API statuses to our predefined Server or Client error classes.
 */
interface ApiStatusResolver {
    fun <T> resolveFailureFromStatus(e: Exception): Response.Failure<T>
}
