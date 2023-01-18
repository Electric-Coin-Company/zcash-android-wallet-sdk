package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork

object FileBlockRangeFixture {
    @Suppress("MagicNumber")
    private val BLOCK_HEIGHT_LOWER_BOUND = BlockHeight.new(ZcashNetwork.Mainnet, 500_000)

    @Suppress("MagicNumber")
    private val BLOCK_HEIGHT_UPPER_BOUND = BlockHeight.new(ZcashNetwork.Mainnet, 500_009)

    fun new(
        lowerBound: BlockHeight = BLOCK_HEIGHT_LOWER_BOUND,
        upperBound: BlockHeight = BLOCK_HEIGHT_UPPER_BOUND
    ): ClosedRange<BlockHeight> {
        return lowerBound..upperBound
    }
}
