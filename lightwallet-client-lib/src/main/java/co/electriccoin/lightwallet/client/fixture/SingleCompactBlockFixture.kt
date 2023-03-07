package co.electriccoin.lightwallet.client.fixture

import cash.z.wallet.sdk.internal.rpc.CompactFormats.CompactBlock
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteStringUtf8

/**
 * Used for getting single mocked compact block for processing and persisting purposes.
 */
internal object SingleCompactBlockFixture {

    const val DEFAULT_BLOCK_HEIGHT = 500_000L

    // Keep this because it makes test assertions easier
    const val DEFAULT_BLOCK_HASH = DEFAULT_BLOCK_HEIGHT

    internal fun heightToFixtureHash(height: Long) = height.toString().toByteStringUtf8()

    fun new(
        blockHeight: Long = DEFAULT_BLOCK_HEIGHT,
        blockHash: ByteString = heightToFixtureHash(blockHeight)
    ): CompactBlock {
        return CompactBlock.newBuilder()
            .setHeight(blockHeight)
            .setHash(blockHash)
            .build()
    }
}
