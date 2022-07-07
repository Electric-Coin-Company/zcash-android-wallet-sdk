package cash.z.ecc.android.sdk.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ZatoshiTest {
    @Test
    fun minValue() {
        assertFailsWith<IllegalArgumentException> {
            Zatoshi(Zatoshi.MIN_INCLUSIVE - 1L)
        }
    }

    @Test
    fun maxValue() {
        assertFailsWith<IllegalArgumentException> {
            Zatoshi(Zatoshi.MAX_INCLUSIVE + 1)
        }
    }

    @Test
    fun plus() {
        assertEquals(Zatoshi(4), Zatoshi(1) + Zatoshi(3))
    }

    @Test
    fun minus() {
        assertEquals(Zatoshi(3), Zatoshi(4) - Zatoshi(1))
    }

    @Test
    fun compare_equal() {
        assertEquals(0, Zatoshi(1).compareTo(Zatoshi(1)))
    }

    @Test
    fun compare_greater() {
        assertTrue(Zatoshi(2) > Zatoshi(1))
    }

    @Test
    fun compare_less() {
        assertTrue(Zatoshi(1) < Zatoshi(2))
    }

    @Test
    fun minus_fail() {
        assertFailsWith<IllegalArgumentException> {
            Zatoshi(5) - Zatoshi(6)
        }
    }
}
