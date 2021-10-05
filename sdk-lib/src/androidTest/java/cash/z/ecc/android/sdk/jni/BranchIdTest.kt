package cash.z.ecc.android.sdk.jni

import cash.z.ecc.android.sdk.annotation.MaintainedTest
import cash.z.ecc.android.sdk.annotation.TestPurpose
import cash.z.ecc.android.sdk.type.NetworkType
import cash.z.ecc.android.sdk.type.ZcashNetwork
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * This test is intended to run to make sure that branch ID logic works across all target devices.
 */
@MaintainedTest(TestPurpose.REGRESSION)
@RunWith(Parameterized::class)
class BranchIdTest(
    private val networkName: String,
    private val height: Int,
    private val branchId: Long,
    private val branchHex: String,
    private val rustBackend: RustBackendWelding
) {

    @Test
    fun testBranchId_Hex() {
        val branchId = rustBackend.getBranchIdForHeight(height)
        val clientBranch = "%x".format(branchId)
        assertEquals("Invalid branch Id Hex value for $networkName at height $height on ${rustBackend.network.networkName}", branchHex, clientBranch)
    }

    @Test
    fun testBranchId_Numeric() {
        val actual = rustBackend.getBranchIdForHeight(height)
        assertEquals("Invalid branch ID for $networkName at height $height on ${rustBackend.network.networkName}", branchId, actual)
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun wallets(): List<Array<Any>> {
            // init values don't matter for this test because we're just checking branchIds, which
            // is an abnormal use of the SDK because this really should run at the rust level
            // However, due to quirks on certain devices, we created this test at the Android level,
            // as a sanity check
            val testnetBackend = RustBackend.init("", "", "", NetworkType.Testnet)
            val mainnetBackend = RustBackend.init("", "", "", NetworkType.Mainnet)
            return listOf(
                // Mainnet Cases
                arrayOf("Sapling", 419_200, 1991772603L, "76b809bb", mainnetBackend),
                arrayOf("Blossom", 653_600, 733220448L, "2bb40e60", mainnetBackend),
                arrayOf("Heartwood", 903_000, 4122551051L, "f5b9230b", mainnetBackend),
                arrayOf("Canopy", 1_046_400, 3925833126L, "e9ff75a6", mainnetBackend),

                // Testnet Cases
                arrayOf("Sapling", 280_000, 1991772603L, "76b809bb", testnetBackend),
                arrayOf("Blossom", 584_000, 733220448L, "2bb40e60", testnetBackend),
                arrayOf("Heartwood", 903_800, 4122551051L, "f5b9230b", testnetBackend),
                arrayOf("Canopy", 1_028_500, 3925833126L, "e9ff75a6", testnetBackend),
            )
        }
    }
}
