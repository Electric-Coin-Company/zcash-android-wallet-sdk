package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.fixture.ZecSendFixture
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MemoTest {
    companion object {
        private const val BYTE_STRING_513 = """
        asdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfa
        """
    }

    @Test
    fun isWithinMaxSize_too_big() {
        assertFalse(Memo.isWithinMaxLength(BYTE_STRING_513))
    }

    @Test
    fun isWithinMaxSize_ok() {
        assertTrue(Memo.isWithinMaxLength(ZecSendFixture.MEMO.value))
    }

    @Test(IllegalArgumentException::class)
    fun init_max_size() {
        Memo(BYTE_STRING_513)
    }
}
