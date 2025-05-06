package co.electriccoin.lightwallet.client

import android.content.Context
import co.electriccoin.lightwallet.client.internal.AndroidChannelFactory
import co.electriccoin.lightwallet.client.internal.LightWalletClientImpl
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface LightWalletClient : BaseWalletClient {
    companion object {
        fun new(
            context: Context,
            lightWalletEndpoint: LightWalletEndpoint,
            singleRequestTimeout: Duration = 10.seconds,
            streamingRequestTimeout: Duration = 90.seconds
        ): LightWalletClient =
            LightWalletClientImpl(
                channelFactory = AndroidChannelFactory(context),
                lightWalletEndpoint = lightWalletEndpoint,
                singleRequestTimeout = singleRequestTimeout,
                streamingRequestTimeout = streamingRequestTimeout
            )
    }
}
