package cash.z.ecc.android.sdk.ext

import cash.z.ecc.android.sdk.internal.ext.BLOCK_HEIGHT_DISCONTINUITY
import cash.z.ecc.android.sdk.internal.ext.PREV_HASH_MISMATCH
import cash.z.ecc.android.sdk.internal.ext.TREE_SIZE_MISMATCH
import cash.z.ecc.android.sdk.internal.ext.isScanContinuityError
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExceptionExtTest {
    @Test
    fun is_scan_continuity_error() {
        assertTrue { RuntimeException(PREV_HASH_MISMATCH).isScanContinuityError() }
        assertTrue { RuntimeException(TREE_SIZE_MISMATCH).isScanContinuityError() }
        assertTrue { RuntimeException(BLOCK_HEIGHT_DISCONTINUITY).isScanContinuityError() }

        assertTrue { RuntimeException(PREV_HASH_MISMATCH.lowercase()).isScanContinuityError() }

        assertTrue { RuntimeException(PREV_HASH_MISMATCH.plus("Text")).isScanContinuityError() }
    }

    @Test
    fun is_not_scan_continuity_error() {
        assertFalse { RuntimeException("Text").isScanContinuityError() }
        assertFalse { RuntimeException("").isScanContinuityError() }
        assertFalse { RuntimeException(PREV_HASH_MISMATCH.drop(1)).isScanContinuityError() }
    }
}
