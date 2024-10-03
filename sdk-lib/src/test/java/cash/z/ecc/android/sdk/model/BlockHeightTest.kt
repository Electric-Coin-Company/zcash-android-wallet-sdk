package cash.z.ecc.android.sdk.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BlockHeightTest {
    @Test
    fun new_mainnet_succeeds_at_sapling_activation_height() {
        BlockHeight.new(ZcashNetwork.Mainnet.saplingActivationHeight.value)
    }

    @Test
    fun new_mainnet_succeeds_above_sapling_activation_height() {
        BlockHeight.new(ZcashNetwork.Mainnet.saplingActivationHeight.value + 10_000)
    }

    @Test
    fun new_mainnet_succeeds_at_max_value() {
        BlockHeight.new(UInt.MAX_VALUE.toLong())
    }

    @Test
    fun new_fails_above_max_value() {
        assertFailsWith(IllegalArgumentException::class) {
            BlockHeight.new(UInt.MAX_VALUE.toLong() + 1)
        }
    }

    @Test
    fun addition_of_int_succeeds() {
        assertEquals(419323L, (ZcashNetwork.Mainnet.saplingActivationHeight + 123).value)
    }

    @Test
    fun addition_of_long_succeeds() {
        assertEquals(419323L, (ZcashNetwork.Mainnet.saplingActivationHeight + 123L).value)
    }

    @Test
    fun subtraction_of_int_fails() {
        assertFailsWith<IllegalArgumentException> {
            ZcashNetwork.Mainnet.saplingActivationHeight + -1
        }
    }

    @Test
    fun subtraction_of_long_fails() {
        assertFailsWith<IllegalArgumentException> {
            ZcashNetwork.Mainnet.saplingActivationHeight + -1L
        }
    }

    @Test
    fun subtraction_of_block_height_succeeds() {
        val one =
            BlockHeight.new(
                ZcashNetwork.Mainnet.saplingActivationHeight.value +
                    ZcashNetwork.Mainnet.saplingActivationHeight.value
            )
        val two = BlockHeight.new(ZcashNetwork.Mainnet.saplingActivationHeight.value)

        assertEquals(ZcashNetwork.Mainnet.saplingActivationHeight.value, one - two)
    }

    @Test
    fun subtraction_of_long_succeeds() {
        assertEquals(
            ZcashNetwork.Mainnet.saplingActivationHeight.value,
            (BlockHeight(419_323L) - 123L).value
        )
    }

    @Test
    fun subtraction_of_int_succeeds() {
        assertEquals(
            ZcashNetwork.Mainnet.saplingActivationHeight.value,
            (BlockHeight(419_323) - 123).value
        )
    }
}
