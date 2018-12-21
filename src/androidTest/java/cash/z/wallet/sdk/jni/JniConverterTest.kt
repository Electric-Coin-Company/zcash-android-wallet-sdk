package cash.z.wallet.sdk.jni

import android.text.format.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Test

class JniConverterTest {

    @Test
    fun testGetAddress_exists() {
        assertNotNull(converter.getAddress("dummyseed".toByteArray()))
    }

    @Test
    fun testGetAddress_valid() {
        val address = converter.getAddress("dummyseed".toByteArray())
        val expectedAddress = "ztestsapling1snmqdnfqnc407pvqw7sld8w5zxx6nd0523kvlj4jf39uvxvh7vn0hs3q38n07806dwwecqwke3t"
        assertEquals("Invalid address", expectedAddress, address)
    }

    @Test
    fun testScanBlocks() {
        // note: for this to work, the db file below must be uploaded to the device. Eventually, this test will be self-contained and remove that requirement.
        val result = converter.scanBlocks(
            "/data/user/0/cash.z.wallet.sdk.test/databases/dummy-cache.db",
            "/data/user/0/cash.z.wallet.sdk.test/databases/data.db",
            "dummyseed".toByteArray(),
            343900
        )
//        Thread.sleep(15 * DateUtils.MINUTE_IN_MILLIS)
        assertEquals("Invalid number of results", 2, 3)
    }

    companion object {
        val converter: JniConverter = JniConverter()

        @BeforeClass
        @JvmStatic
        fun setup() {
            converter.initLogs()
        }
    }

}