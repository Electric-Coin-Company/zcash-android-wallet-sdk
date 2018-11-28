package cash.z.wallet.sdk.jni

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class JniConverterTest {

    val converter:JniConverter = JniConverter()

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

}