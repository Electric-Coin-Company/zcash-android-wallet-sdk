package cash.z.ecc.android.sdk.jni

import cash.z.ecc.android.sdk.annotation.MaintainedTest
import cash.z.ecc.android.sdk.annotation.TestPurpose
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
    private val branchHex: String,
    private val rustBackend: RustBackendWelding
) {

    @Test
    fun testBranchId() {
        val branchId = rustBackend.getBranchIdForHeight(height)
        val clientBranch = "%x".format(branchId)
        assertEquals("Invalid branch ID for $networkName at height $height on ${rustBackend.network.networkName}", branchHex, clientBranch)
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun wallets(): List<Array<Any>> {
            // init values don't matter for this test because we're just checking branchIds, which
            // is an abnormal use of the SDK because this really should run at the rust level
            // However, due to quirks on certain devices, we created this test at the Android level,
            // as a sanity check
            val testnetBackend = RustBackend.init("", "", "", ZcashNetwork.Testnet)
            val mainnetBackend = RustBackend.init("", "", "", ZcashNetwork.Mainnet)
            return listOf(
                // Mainnet Cases
                arrayOf("Sapling", 419_200, "76b809bb", mainnetBackend),
                arrayOf("Blossom", 653_600, "2bb40e60", mainnetBackend),
                arrayOf("Heartwood", 903_000, "f5b9230b", mainnetBackend),
                arrayOf("Canopy", 1_046_400, "e9ff75a6", mainnetBackend),

                // Testnet Cases
                arrayOf("Sapling", 280_000, "76b809bb", testnetBackend),
                arrayOf("Blossom", 584_000, "2bb40e60", testnetBackend),
                arrayOf("Heartwood", 903_800, "f5b9230b", testnetBackend),
                arrayOf("Canopy", 1_028_500, "e9ff75a6", testnetBackend),
            )
        }
    }
}
