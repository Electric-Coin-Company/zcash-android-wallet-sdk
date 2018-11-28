package cash.z.wallet.sdk.jni

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class JniConverterTest {

    val converter: JniConverter = JniConverter()

    @Before
    fun initLogs() {
        converter.initLogs()
    }

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
        val result = converter.scanBlocks(
            "/data/user/0/cash.z.wallet.sdk.test/databases/dummy-cache.db",
            343900,
            354446,
            "dummyseed".toByteArray()
        )
        val expectedAddress = "ztestsapling1snmqdnfqnc407pvqw7sld8w5zxx6nd0523kvlj4jf39uvxvh7vn0hs3q38n07806dwwecqwke3t"
        assertEquals(5, result.size)
    }

}