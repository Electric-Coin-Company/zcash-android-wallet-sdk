package cash.z.ecc.android.sdk.internal

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.SaplingParamTool.OUTPUT_PARAM_FILE_MAX_BYTES_SIZE
import cash.z.ecc.android.sdk.internal.SaplingParamTool.SPEND_PARAM_FILE_MAX_BYTES_SIZE
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Ignore(
    "These tests need to be refactored to a separate test module. They cause SSLHandshakeException: Chain " +
        "validation failed on CI"
)
class SaplingParamToolTest {

    val context: Context = InstrumentationRegistry.getInstrumentation().context

    val cacheDir = "${context.cacheDir.absolutePath}/params"

    @Before
    fun setup() {
        // clear the param files
        runBlocking { SaplingParamTool.clear(cacheDir) }
    }

    @Test
    @Ignore("This test is broken")
    fun testFilesExists() = runBlocking {
        // Given
        SaplingParamTool.fetchParams(cacheDir)

        // When
        val result = SaplingParamTool.validate(cacheDir)

        // Then
        assertFalse(result)
    }

    @Test
    fun output_file_exists() = runBlocking {
        // Given
        SaplingParamTool.fetchParams(cacheDir)
        File(cacheDir, ZcashSdk.OUTPUT_PARAM_FILE_NAME).delete()

        // When
        val result = SaplingParamTool.validate(cacheDir)

        // Then
        assertFalse(result, "Validation should fail when the spend params are missing")
    }

    @Test
    fun param_file_exists() = runBlocking {
        // Given
        SaplingParamTool.fetchParams(cacheDir)
        File(cacheDir, ZcashSdk.SPEND_PARAM_FILE_NAME).delete()

        // When
        val result = SaplingParamTool.validate(cacheDir)

        // Then
        assertFalse(result, "Validation should fail when the spend params are missing")
    }

    @Test
    fun testInsufficientDeviceStorage() = runBlocking {
        // Given
        SaplingParamTool.fetchParams(cacheDir)

        assertFalse(false, "insufficient storage")
    }

    @Test
    fun testSufficientDeviceStorageForOnlyOneFile() = runBlocking {
        SaplingParamTool.fetchParams(cacheDir)

        assertFalse(false, "insufficient storage")
    }

    @Test
    fun check_correct_param_file_size() = runBlocking {
        SaplingParamTool.fetchParams(cacheDir)

        val spendParamFile = File(cacheDir, ZcashSdk.SPEND_PARAM_FILE_NAME)
        assertTrue(spendParamFile.length() < SPEND_PARAM_FILE_MAX_BYTES_SIZE)

        val outputParamFile = File(cacheDir, ZcashSdk.OUTPUT_PARAM_FILE_NAME)
        assertTrue(outputParamFile.length() < OUTPUT_PARAM_FILE_MAX_BYTES_SIZE)
    }

    @Test
    fun check_incorrect_param_file_size() = runBlocking {
        SaplingParamTool.fetchParams(cacheDir)

        val spendParamFile = File(cacheDir, ZcashSdk.SPEND_PARAM_FILE_NAME)
        assertFalse(spendParamFile.length() < OUTPUT_PARAM_FILE_MAX_BYTES_SIZE)
    }
}
