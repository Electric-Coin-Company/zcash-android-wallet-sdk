package co.electriccoin.lightwallet.client.fixture

import androidx.annotation.VisibleForTesting
import cash.z.wallet.sdk.internal.rpc.CompactFormats.CompactBlock
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe

/**
 * Used for getting mocked blocks list for processing and persisting compact blocks purposes.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
object ListOfCompactBlocksFixture {

    private val DEFAULT_FILE_BLOCK_RANGE = FileBlockRangeFixture.new()

    fun new(
        blocksHeightRange: ClosedRange<BlockHeightUnsafe> = DEFAULT_FILE_BLOCK_RANGE
    ): Sequence<CompactBlock> {
        val blocks = mutableListOf<CompactBlock>()

        for (blockHeight in blocksHeightRange.start.value..blocksHeightRange.endInclusive.value) {
            blocks.add(
                SingleCompactBlockFixture.new(blockHeight = blockHeight)
            )
        }

        return blocks.asSequence()
    }
}
