package cash.z.ecc.android.sdk.ext

import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.internal.ext.createNewFileSuspend
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.getSha1Hash
import cash.z.ecc.android.sdk.test.getAppContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileExtTest {
    private val testFile = File(getAppContext().filesDir, "test_file")

    @Before
    @After
    fun remove_test_files() {
        testFile.delete()
    }

    @Test
    @SmallTest
    @OptIn(ExperimentalCoroutinesApi::class)
    fun check_empty_file_sha1_result() =
        runTest {
            testFile.apply {
                createNewFileSuspend()
                assertTrue(existsSuspend())
                assertEquals(
                    expected = "da39a3ee5e6b4b0d3255bfef95601890afd80709",
                    actual = getSha1Hash(),
                    message = "SHA1 hashes are not the same."
                )
            }
        }

    @Test
    @SmallTest
    @OptIn(ExperimentalCoroutinesApi::class)
    fun check_not_empty_file_sha1_result() =
        runTest {
            testFile.apply {
                createNewFileSuspend()
                assertTrue(existsSuspend())
                writeText("Hey! It compiles! Ship it!")
                assertTrue(length() > 0)
                assertEquals(
                    expected = "28756ec5d3a73f1e8993bdd46de74b79453ff21c",
                    actual = getSha1Hash(),
                    message = "SHA1 hashes are not the same."
                )
            }
        }
}
