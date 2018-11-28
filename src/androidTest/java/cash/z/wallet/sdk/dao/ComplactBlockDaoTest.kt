package cash.z.wallet.sdk.dao

import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComplactBlockDaoTest {

    private var dao: CompactBlockDao? = null

    @Test
    fun testDaoExists() {
        assertNotNull(dao)
    }
}