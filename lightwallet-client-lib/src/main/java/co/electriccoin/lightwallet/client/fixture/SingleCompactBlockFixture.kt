package co.electriccoin.lightwallet.client.fixture

import android.R.attr.value
import co.electriccoin.lightwallet.client.model.CompactBlockUnsafe
import co.electriccoin.lightwallet.client.model.CompactTxUnsafe
import java.nio.ByteBuffer

/**
 * Used for getting single mocked compact block for processing and persisting purposes.
 */
internal object SingleCompactBlockFixture {

    internal const val DEFAULT_PROTO_VERSION = 1
    internal const val DEFAULT_HEIGHT = 500_000L
    internal const val DEFAULT_TIME = 0

    internal const val DEFAULT_HASH = DEFAULT_HEIGHT
    internal const val DEFAULT_PREV_HASH = DEFAULT_HEIGHT
    internal const val DEFAULT_HEADER = DEFAULT_HEIGHT
    internal fun heightToFixtureData(height: Long) = BytesConversionHelper.longToBytes(height)
    internal fun fixtureDataToHeight(byteArray: ByteArray) = BytesConversionHelper.bytesToLong(byteArray)

    // We could fill with a fixture value if needed for testing
    internal val DEFAULT_VTX = emptyList<CompactTxUnsafe>()

    @Suppress("LongParameterList")
    fun new(
        protoVersion: Int = DEFAULT_PROTO_VERSION,
        height: Long = DEFAULT_HEIGHT,
        hash: ByteArray = heightToFixtureData(height),
        prevHash: ByteArray = heightToFixtureData(height),
        time: Int = DEFAULT_TIME,
        header: ByteArray = heightToFixtureData(height),
        vtxList: List<CompactTxUnsafe> = DEFAULT_VTX
    ): CompactBlockUnsafe {
        return CompactBlockUnsafe(
            protoVersion = protoVersion,
            height = height,
            hash = hash,
            prevHash = prevHash,
            time = time,
            header = header,
            vtx = vtxList
        )
    }
}

private object BytesConversionHelper {
    private fun getBuffer(): ByteBuffer {
        return ByteBuffer.allocate(java.lang.Long.BYTES)
    }

    fun longToBytes(x: Long): ByteArray {
        val buffer = getBuffer()
        buffer.putLong(0, x)
        return buffer.array()
    }

    fun bytesToLong(bytes: ByteArray): Long {
        val buffer = getBuffer()
        buffer.put(bytes, 0, bytes.size)
        buffer.flip()
        return buffer.long
    }
}
