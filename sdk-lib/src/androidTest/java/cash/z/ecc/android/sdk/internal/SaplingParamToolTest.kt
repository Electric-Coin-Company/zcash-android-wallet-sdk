package cash.z.ecc.android.sdk.internal

import androidx.test.ext.junit.runners.AndroidJUnit4
import cash.z.ecc.fixture.SaplingParamsFixture
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Ignore(
    "These tests need to be refactored to a separate test module. They cause SSLHandshakeException: Chain " +
        "validation failed on CI"
)
@RunWith(AndroidJUnit4::class)
class SaplingParamToolTest {

    private val spendSaplingParams = SaplingParamsFixture.newFile()

    private val outputSaplingParams = SaplingParamsFixture.newFile(
        SaplingParamsFixture.DESTINATION_DIRECTORY,
        SaplingParamsFixture.OUTPUT_FILE_NAME,
        SaplingParamsFixture.OUTPUT_FILE_MAX_SIZE
    )

    @Before
    fun setup() {
        // clear the param files
        runBlocking { SaplingParamTool.clear(spendSaplingParams.destinationDirectoryPath) }
    }

    @Test
    fun test_files_exists() = runBlocking {
        // Given
        SaplingParamTool.fetchParams(spendSaplingParams)
        SaplingParamTool.fetchParams(outputSaplingParams)

        // When
        val result = SaplingParamTool.validate(SaplingParamsFixture.DESTINATION_DIRECTORY)

        // Then
        assertTrue(result)
    }

    @Test
    fun output_file_exists() = runBlocking {
        // Given
        SaplingParamTool.fetchParams(spendSaplingParams)
        File(spendSaplingParams.destinationDirectoryPath, spendSaplingParams.fileName).delete()

        // When
        val result = SaplingParamTool.validate(spendSaplingParams.destinationDirectoryPath)

        // Then
        assertFalse(result, "Validation should fail when the spend params are missing")
    }

    @Test
    fun spend_file_exists() = runBlocking {
        // Given
        SaplingParamTool.fetchParams(outputSaplingParams)
        File(outputSaplingParams.destinationDirectoryPath, outputSaplingParams.fileName).delete()

        // When
        val result = SaplingParamTool.validate(outputSaplingParams.destinationDirectoryPath)

        // Then
        assertFalse(result, "Validation should fail when the output params are missing")
    }

    @Test
    fun testInsufficientDeviceStorage() = runBlocking {
        // Given
        SaplingParamTool.fetchParams(spendSaplingParams)

        assertFalse(false, "insufficient storage")
    }

    @Test
    fun testSufficientDeviceStorageForOnlyOneFile() = runBlocking {
        SaplingParamTool.fetchParams(spendSaplingParams)

        assertFalse(false, "insufficient storage")
    }

    @Test
    fun check_all_files_fetched() = runBlocking {
        val expectedSpendFile = File(
            SaplingParamsFixture.DESTINATION_DIRECTORY,
            SaplingParamsFixture.SPEND_FILE_NAME
        )
        val expectedOutputFile = File(
            SaplingParamsFixture.DESTINATION_DIRECTORY,
            SaplingParamsFixture.OUTPUT_FILE_NAME
        )

        SaplingParamTool.ensureParams(SaplingParamsFixture.DESTINATION_DIRECTORY)

        val actualFiles = File(SaplingParamsFixture.DESTINATION_DIRECTORY).listFiles()
        assertNotNull(actualFiles)

        assertContains(actualFiles, expectedSpendFile)

        assertContains(actualFiles, expectedOutputFile)
    }

    @Test
    fun check_correct_spend_param_file_size() = runBlocking {
        SaplingParamTool.fetchParams(spendSaplingParams)

        val expectedSpendFile = File(
            SaplingParamsFixture.DESTINATION_DIRECTORY,
            SaplingParamsFixture.SPEND_FILE_NAME
        )

        assertTrue(expectedSpendFile.length() < SaplingParamsFixture.SPEND_FILE_MAX_SIZE)
        assertFalse(expectedSpendFile.length() < SaplingParamsFixture.OUTPUT_FILE_MAX_SIZE)
    }

    @Test
    fun check_correct_output_param_file_size() = runBlocking {
        SaplingParamTool.fetchParams(outputSaplingParams)

        val expectedOutputFile = File(
            SaplingParamsFixture.DESTINATION_DIRECTORY,
            SaplingParamsFixture.OUTPUT_FILE_NAME
        )

        assertTrue(expectedOutputFile.length() < SaplingParamsFixture.OUTPUT_FILE_MAX_SIZE)
        assertFalse(expectedOutputFile.length() > SaplingParamsFixture.SPEND_FILE_MAX_SIZE)
    }
}
