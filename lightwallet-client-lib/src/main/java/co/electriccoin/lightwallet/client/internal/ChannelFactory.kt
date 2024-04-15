package co.electriccoin.lightwallet.client.internal

import android.util.Log
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import io.grpc.ManagedChannel
import io.grpc.android.AndroidChannelBuilder

internal interface ChannelFactory {
    fun newChannel(endpoint: LightWalletEndpoint): ManagedChannel
}

internal class AndroidChannelFactory(context: android.content.Context) : ChannelFactory {
    private val context = context.applicationContext

    override fun newChannel(endpoint: LightWalletEndpoint): ManagedChannel {
        return AndroidChannelBuilder
            .forAddress(endpoint.host, endpoint.port)
            .context(context)
            .apply {
                if (endpoint.isSecure) {
                    useTransportSecurity()
                } else {
                    Log.w(Constants.LOG_TAG, "WARNING Using plaintext connection")
                    usePlaintext()
                }
            }
            .build()
    }
}
