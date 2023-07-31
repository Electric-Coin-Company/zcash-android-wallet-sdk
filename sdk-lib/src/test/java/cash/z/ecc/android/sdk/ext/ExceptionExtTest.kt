package cash.z.ecc.android.sdk.ext

import cash.z.ecc.android.sdk.internal.ext.PREV_HASH_MISMATCH
import cash.z.ecc.android.sdk.internal.ext.isScanContinuityError
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExceptionExtTest {

    @Test
    fun is_scan_continuity_error() {
        assertTrue {
            RuntimeException(PREV_HASH_MISMATCH).isScanContinuityError()
        }
    }

    @Test
    fun is_not_scan_continuity_error() {
        assertFalse {
            RuntimeException("Text").isScanContinuityError()
        }
    }
}
