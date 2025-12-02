package co.electriccoin.lightwallet.client.model

class ResponseException(
    val code: Int,
    val description: String?,
    cause: Throwable,
    message: String = "Communication failure with details: $code${description?.let { ": $it" } ?: "."}",
) : Exception(message, cause)
