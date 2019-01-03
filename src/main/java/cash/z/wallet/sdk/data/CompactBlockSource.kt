package cash.z.wallet.sdk.data

import kotlinx.coroutines.channels.ReceiveChannel
import rpc.CompactFormats.CompactBlock

interface CompactBlockSource {
    fun blocks(): ReceiveChannel<Result<CompactBlock>>
}