package co.electriccoin.lightwallet.client.model

/**
 * A Block Height that has come from the Light Wallet server.
 *
 * It is marked as "unsafe" because it is not guaranteed to be valid.
 */
data class BlockHeightUnsafe(val value: Long) : Comparable<BlockHeightUnsafe> {
    init {
        require(UINT_RANGE.contains(value)) { "Height $value is outside of allowed range $UINT_RANGE" }
    }

    override fun compareTo(other: BlockHeightUnsafe): Int = value.compareTo(other.value)

    companion object {
        private val UINT_RANGE = 0.toLong()..UInt.MAX_VALUE.toLong()
    }
}
