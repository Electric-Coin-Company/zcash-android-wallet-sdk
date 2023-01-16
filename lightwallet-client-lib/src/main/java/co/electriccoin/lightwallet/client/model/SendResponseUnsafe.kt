package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.Service

data class SendResponseUnsafe(
    val code: Int,
    val message: String
) {
    companion object {
        internal fun new(sendResponse: Service.SendResponse) =
            SendResponseUnsafe(
                code = sendResponse.errorCode,
                message = sendResponse.errorMessage
            )
    }
}
