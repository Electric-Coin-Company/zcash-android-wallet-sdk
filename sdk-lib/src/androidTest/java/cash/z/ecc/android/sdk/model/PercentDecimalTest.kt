package cash.z.ecc.android.sdk.model

import androidx.test.filters.SmallTest
import org.junit.Test
import kotlin.test.assertTrue

class PercentDecimalTest {

    @Test(expected = IllegalArgumentException::class)
    @SmallTest
    fun require_greater_than_zero() {
        PercentDecimal(-1.0f)
    }

    @Test(expected = IllegalArgumentException::class)
    @SmallTest
    fun require_less_than_one() {
        PercentDecimal(1.5f)
    }

    @SmallTest
    @Test
    fun is_less_than_hundred_percent_test() {
        assertTrue(PercentDecimal(0.5f).isLessThanHundredPercent())
    }

    @SmallTest
    @Test
    fun is_more_than_zero_percent_test() {
        assertTrue(PercentDecimal(0.5f).isMoreThanZeroPercent())
    }

    @SmallTest
    @Test
    fun to_percentage_test() {
        assertTrue(PercentDecimal(0.5f).toPercentage() == 50)
    }
}
