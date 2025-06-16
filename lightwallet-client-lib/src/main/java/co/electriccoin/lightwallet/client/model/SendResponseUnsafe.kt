package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.Service

/**
 * A SendResponse encodes an error code and a string. It is currently used only by SendTransaction(). If error code
 * is zero, the operation was successful; if non-zero, it and the message specify the failure. It has come from the
 * Light Wallet server.
 *
 * It is marked as "unsafe" because it is not guaranteed to be valid.
 */
data class SendResponseUnsafe(
    val code: Int,
    val message: String
) {
    companion object {
        fun new(sendResponse: Service.SendResponse) =
            SendResponseUnsafe(
                code = sendResponse.errorCode,
                message = sendResponse.errorMessage
            )
    }
}
