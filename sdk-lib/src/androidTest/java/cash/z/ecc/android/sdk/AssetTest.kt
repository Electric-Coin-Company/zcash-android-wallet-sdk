package cash.z.ecc.android.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool
import cash.z.ecc.android.sdk.type.NetworkType
import cash.z.ecc.android.sdk.type.ZcashNetwork
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetTest {

    @Test
    @SmallTest
    fun validate_mainnet_assets() {
        val network = NetworkType.Mainnet
        val assets = listAssets(network)

        assertFilesExist(assets)
        assertFilenames(assets)
        assertFileContents(network, assets)
    }

    @Test
    @SmallTest
    fun validate_testnet_assets() {
        val network = NetworkType.Testnet
        val assets = listAssets(network)

        assertFilesExist(assets)
        assertFilenames(assets)
        assertFileContents(network, assets)
    }

    private fun assertFilesExist(files: Array<String>?) {
        assertFalse(files.isNullOrEmpty())
    }

    private fun assertFilenames(files: Array<String>?) {
        files?.forEach {
            val split = it.split('.')
            assertEquals(2, split.size)

            val intString = split.first()
            val extensionString = split.last()

            // Will throw exception if cannot be parsed
            intString.toInt()

            assertEquals("json", extensionString)
        }
    }

    private fun assertFileContents(network: ZcashNetwork, files: Array<String>?) {
        files?.map { filename ->
            val filePath = "${WalletBirthdayTool.birthdayDirectory(network)}/$filename"
            ApplicationProvider.getApplicationContext<Context>().assets.open(filePath)
                .use { inputSteam ->
                    inputSteam.bufferedReader().use { bufferedReader ->
                        val slurped = bufferedReader.readText()

                        JsonFile(JSONObject(slurped), filename)
                    }
                }
        }?.forEach {
            val jsonObject = it.jsonObject
            assertTrue(jsonObject.has("network"))
            assertTrue(jsonObject.has("height"))
            assertTrue(jsonObject.has("hash"))
            assertTrue(jsonObject.has("time"))
            assertTrue(jsonObject.has("tree"))

            val expectedNetworkName = when (network) {
                NetworkType.Mainnet -> "main"
                NetworkType.Testnet -> "test"
                else -> throw AssertionError("Unknown network $network")
            }
            assertEquals("File: ${it.filename}", expectedNetworkName, jsonObject.getString("network"))

            assertEquals(
                "File: ${it.filename}",
                WalletBirthdayTool.birthdayHeight(it.filename),
                jsonObject.getInt("height")
            )

            // In the future, additional validation of the JSON can be added
        }
    }

    private data class JsonFile(val jsonObject: JSONObject, val filename: String)

    companion object {
        fun listAssets(network: ZcashNetwork) = WalletBirthdayTool.listBirthdayDirectoryContents(
            ApplicationProvider.getApplicationContext<Context>(),
            WalletBirthdayTool.birthdayDirectory(network)
        )
    }
}
