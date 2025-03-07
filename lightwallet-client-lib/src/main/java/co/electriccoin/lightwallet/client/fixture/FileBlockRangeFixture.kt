package co.electriccoin.lightwallet.client.fixture

import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe

/**
 * Used for getting mocked blocks range for processing and persisting compact blocks purposes.
 */
internal object FileBlockRangeFixture {
    @Suppress("MagicNumber")
    private val BLOCK_HEIGHT_LOWER_BOUND = BlockHeightUnsafe(500_000L)

    @Suppress("MagicNumber")
    private val BLOCK_HEIGHT_UPPER_BOUND = BlockHeightUnsafe(500_009L)

    fun new(
        lowerBound: BlockHeightUnsafe = BLOCK_HEIGHT_LOWER_BOUND,
        upperBound: BlockHeightUnsafe = BLOCK_HEIGHT_UPPER_BOUND
    ): ClosedRange<BlockHeightUnsafe> = lowerBound..upperBound
}
