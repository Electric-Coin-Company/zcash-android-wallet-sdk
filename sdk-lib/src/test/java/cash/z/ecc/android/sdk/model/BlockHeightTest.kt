package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.type.ZcashNetwork
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BlockHeightTest {
    @Test
    fun new_mainnet_fails_below_sapling_activation_height() {
        assertFailsWith(IllegalArgumentException::class) {
            BlockHeight.new(
                ZcashNetwork.Mainnet,
                ZcashNetwork.Mainnet.saplingActivationHeight.value - 1
            )
        }
    }

    @Test
    fun new_mainnet_succeeds_at_sapling_activation_height() {
        BlockHeight.new(ZcashNetwork.Mainnet, ZcashNetwork.Mainnet.saplingActivationHeight.value)
    }

    @Test
    fun new_mainnet_succeeds_above_sapling_activation_height() {
        BlockHeight.new(ZcashNetwork.Mainnet, ZcashNetwork.Mainnet.saplingActivationHeight.value + 10_000)
    }

    @Test
    fun new_mainnet_succeeds_at_max_value() {
        BlockHeight.new(ZcashNetwork.Mainnet, UInt.MAX_VALUE.toLong())
    }

    @Test
    fun new_fails_above_max_value() {
        assertFailsWith(IllegalArgumentException::class) {
            BlockHeight.new(ZcashNetwork.Mainnet, UInt.MAX_VALUE.toLong() + 1)
        }
    }

    @Test
    fun addition_succeeds() {
        val one = BlockHeight.new(ZcashNetwork.Mainnet, ZcashNetwork.Mainnet.saplingActivationHeight.value)
        val two = BlockHeight.new(ZcashNetwork.Mainnet, ZcashNetwork.Mainnet.saplingActivationHeight.value + 123)

        assertEquals(838523L, (one + two).value)
    }
}
