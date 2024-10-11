package co.electriccoin.lightwallet.client.model

/**
 * A Block Height that has come from the Light Wallet server.
 *
 * It is marked as "unsafe" because it is not guaranteed to be valid.
 */
data class BlockHeightUnsafe(val value: ULong) : Comparable<BlockHeightUnsafe> {
    init {
        require(ULONG_RANGE.contains(value)) { "Height $value is outside of allowed range $ULONG_RANGE" }
    }

    override fun compareTo(other: BlockHeightUnsafe): Int = value.compareTo(other.value)

    companion object {
        private val ULONG_RANGE: ULongRange = 0.toULong()..UInt.MAX_VALUE.toULong()
    }
}
