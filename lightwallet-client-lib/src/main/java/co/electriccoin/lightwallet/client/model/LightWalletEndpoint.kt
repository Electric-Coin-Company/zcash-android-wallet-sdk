package co.electriccoin.lightwallet.client.model

data class LightWalletEndpoint(val host: String, val port: Int, val isSecure: Boolean) {
    companion object
}
