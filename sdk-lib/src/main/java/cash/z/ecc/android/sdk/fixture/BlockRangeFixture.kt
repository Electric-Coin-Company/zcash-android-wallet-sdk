package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork

object BlockRangeFixture {

    @Suppress("MagicNumber")
    private val BLOCK_HEIGHT_LOWER_BOUND = BlockHeight.new(ZcashNetwork.Mainnet, 1880001L)

    @Suppress("MagicNumber")
    private val BLOCK_HEIGHT_UPPER_BOUND = BlockHeight.new(ZcashNetwork.Mainnet, 1880100L)

    fun new(
        lowerBound: BlockHeight = BLOCK_HEIGHT_LOWER_BOUND,
        upperBound: BlockHeight = BLOCK_HEIGHT_UPPER_BOUND
    ): ClosedRange<BlockHeight> {
        return lowerBound..upperBound
    }
}
