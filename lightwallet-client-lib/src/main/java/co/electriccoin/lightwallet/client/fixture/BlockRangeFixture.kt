package co.electriccoin.lightwallet.client.fixture

object BlockRangeFixture {

    // Be aware that changing these bounds values in a broader range may result in a timeout reached in
    // SyncBlockchainBenchmark. So if changing these, don't forget to align also the test timeout in
    // waitForBalanceScreen() appropriately.

    @Suppress("MagicNumber")
    private val BLOCK_HEIGHT_LOWER_BOUND = 1730001L

    @Suppress("MagicNumber")
    private val BLOCK_HEIGHT_UPPER_BOUND = 1730100L

    fun new(
        lowerBound: Long = BLOCK_HEIGHT_LOWER_BOUND,
        upperBound: Long = BLOCK_HEIGHT_UPPER_BOUND
    ): ClosedRange<Long> {
        return lowerBound..upperBound
    }
}
