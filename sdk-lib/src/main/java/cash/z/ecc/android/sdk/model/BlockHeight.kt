package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.type.ZcashNetwork

/**
 * Represents a block height, which is a UInt32.  SDK clients use this class to represent the "birthday" of a wallet.
 *
 * @param value The block height.  Must be in range of a UInt32.
 */
/*
 * For compatibility with Java clients, this class represents the height value as a Long with
 * assertions to ensure that it is a 32-bit unsigned integer.
 */
data class BlockHeight internal constructor(val value: Long) : Comparable<BlockHeight> {
    init {
        require(UINT_RANGE.contains(value)) { "Height $value is outside of allowed range $UINT_RANGE" }
    }

    override fun compareTo(other: BlockHeight): Int = value.compareTo(other.value)

    operator fun plus(other: BlockHeight) = BlockHeight(value + other.value)

    internal operator fun plus(other: Int): BlockHeight {
        // -1 also isn't valid, but we have a few use cases inside the SDK where we do a -1 so we need to allow that.
        if (other < -1) {
            throw IllegalArgumentException("Cannot add negative value $other to BlockHeight")
        }

        return BlockHeight(value + other.toLong())
    }

    internal operator fun plus(other: Long): BlockHeight {
        // -1 also isn't valid, but we have a few use cases inside the SDK where we do a -1 so we need to allow that.
        if (other < -1) {
            throw IllegalArgumentException("Cannot add negative value $other to BlockHeight")
        }

        return BlockHeight(value + other)
    }

    companion object {
        private val UINT_RANGE = 0.toLong()..UInt.MAX_VALUE.toLong()

        /**
         * @param zcashNetwork Network to use for the block height.
         * @param blockHeight The block height.  Must be in range of a UInt32 AND must be greater than the network's sapling activation height.
         */
        fun new(zcashNetwork: ZcashNetwork, blockHeight: Long): BlockHeight {
            require(blockHeight >= zcashNetwork.saplingActivationHeight.value) {
                "Height $blockHeight is below sapling activation height ${zcashNetwork.saplingActivationHeight}"
            }

            return BlockHeight(blockHeight)
        }
    }
}
