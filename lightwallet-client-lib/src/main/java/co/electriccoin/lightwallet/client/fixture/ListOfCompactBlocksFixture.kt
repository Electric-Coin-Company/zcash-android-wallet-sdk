package co.electriccoin.lightwallet.client.fixture

import androidx.annotation.VisibleForTesting
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.CompactBlockUnsafe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

/**
 * Used for getting mocked blocks list for processing and persisting compact blocks purposes.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
object ListOfCompactBlocksFixture {
    val DEFAULT_FILE_BLOCK_RANGE = FileBlockRangeFixture.new()

    @Suppress("MaxLineLength")
    fun newSequence(blocksHeightRange: ClosedRange<BlockHeightUnsafe> = DEFAULT_FILE_BLOCK_RANGE): Sequence<CompactBlockUnsafe> {
        val blocks = mutableListOf<CompactBlockUnsafe>()

        for (blockHeight in blocksHeightRange.start.value..blocksHeightRange.endInclusive.value) {
            blocks.add(
                SingleCompactBlockFixture.new(height = blockHeight)
            )
        }

        return blocks.asSequence()
    }

    @Suppress("MaxLineLength")
    fun newFlow(blocksHeightRange: ClosedRange<BlockHeightUnsafe> = DEFAULT_FILE_BLOCK_RANGE): Flow<CompactBlockUnsafe> =
        newSequence(blocksHeightRange).asFlow()
}
