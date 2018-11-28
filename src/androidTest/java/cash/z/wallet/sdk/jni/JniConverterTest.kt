package cash.z.wallet.sdk.jni

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
            "/data/user/0/cash.z.wallet.sdk.test/databases/compact-block.db",
            343900,
            344855,
            "dummyseed".toByteArray()
        )
        assertEquals("Invalid number of results", 2, result.size)
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