package co.electriccoin.lightwallet.client.model

class UninitializedTorClientException(
    cause: Exception
) : RuntimeException(cause)
