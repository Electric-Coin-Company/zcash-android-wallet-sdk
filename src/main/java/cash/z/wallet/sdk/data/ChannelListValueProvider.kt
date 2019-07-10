package cash.z.android.wallet.data

import kotlinx.coroutines.channels.ConflatedBroadcastChannel

class ChannelListValueProvider<T>(val channel: ConflatedBroadcastChannel<List<T>>) {
    fun getLatestValue(): List<T> {
        return if (channel.isClosedForSend) listOf() else channel.value
    }
}
