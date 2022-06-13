package cash.z.ecc.android.sdk.internal

import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.type.WalletBirthday
import cash.z.ecc.fixture.WalletBirthdayFixture
import cash.z.ecc.fixture.toJson
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletBirthdayTest {
    @Test
    @SmallTest
    fun deserialize() {
        val fixtureBirthday = WalletBirthdayFixture.new()

        val deserialized = WalletBirthday.from(fixtureBirthday.toJson())

        assertEquals(fixtureBirthday, deserialized)
    }

    @Test
    @SmallTest
    fun epoch_seconds_as_long_that_would_overflow_int() {
        val jsonString = WalletBirthdayFixture.new(time = Long.MAX_VALUE).toJson()

        WalletBirthday.from(jsonString).also {
            assertEquals(Long.MAX_VALUE, it.time)
        }
    }
}
