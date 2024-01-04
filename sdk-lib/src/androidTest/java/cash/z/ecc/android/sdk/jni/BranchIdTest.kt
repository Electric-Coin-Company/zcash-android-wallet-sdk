package cash.z.ecc.android.sdk.jni

import cash.z.ecc.android.sdk.annotation.MaintainedTest
import cash.z.ecc.android.sdk.annotation.TestPurpose
import cash.z.ecc.android.sdk.internal.TypesafeBackend
import cash.z.ecc.android.sdk.internal.TypesafeBackendImpl
import cash.z.ecc.android.sdk.internal.jni.RustBackend
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/**
 * This test is intended to run to make sure that branch ID logic works across all target devices.
 */
@MaintainedTest(TestPurpose.REGRESSION)
@RunWith(Parameterized::class)
class BranchIdTest internal constructor(
    private val networkName: String,
    private val height: BlockHeight,
    private val branchId: Long,
    private val branchHex: String,
    private val backend: TypesafeBackend
) {
    @Test
    fun testBranchId_Hex() {
        val branchId = backend.getBranchIdForHeight(height)
        val clientBranch = "%x".format(branchId)
        assertEquals(
            "Invalid branch Id Hex value for $networkName at height $height on ${backend.network.networkName}",
            branchHex,
            clientBranch
        )
    }

    @Test
    fun testBranchId_Numeric() {
        val actual = backend.getBranchIdForHeight(height)
        assertEquals(
            "Invalid branch ID for $networkName at height $height on ${backend.network.networkName}",
            branchId,
            actual
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        @Suppress("LongMethod")
        fun wallets(): List<Array<Any>> {
            // init values don't matter for this test because we're just checking branchIds, which
            // is an abnormal use of the SDK because this really should run at the rust level
            // However, due to quirks on certain devices, we created this test at the Android level,
            // as a sanity check
            val testnetBackend =
                runBlocking {
                    TypesafeBackendImpl(
                        RustBackend.new(
                            File(""),
                            File(""),
                            File(""),
                            File(""),
                            ZcashNetwork.Testnet.id,
                        )
                    )
                }
            val mainnetBackend =
                runBlocking {
                    TypesafeBackendImpl(
                        RustBackend.new(
                            File(""),
                            File(""),
                            File(""),
                            File(""),
                            ZcashNetwork.Mainnet.id,
                        )
                    )
                }
            return listOf(
                // Mainnet Cases
                arrayOf(
                    "Sapling",
                    BlockHeight.new(ZcashNetwork.Mainnet, 419_200),
                    1991772603L,
                    "76b809bb",
                    mainnetBackend
                ),
                arrayOf(
                    "Blossom",
                    BlockHeight.new(ZcashNetwork.Mainnet, 653_600),
                    733220448L,
                    "2bb40e60",
                    mainnetBackend
                ),
                arrayOf(
                    "Heartwood",
                    BlockHeight.new(ZcashNetwork.Mainnet, 903_000),
                    4122551051L,
                    "f5b9230b",
                    mainnetBackend
                ),
                arrayOf(
                    "Canopy",
                    BlockHeight.new(ZcashNetwork.Mainnet, 1_046_400),
                    3925833126L,
                    "e9ff75a6",
                    mainnetBackend
                ),
                // Testnet Cases
                arrayOf(
                    "Sapling",
                    BlockHeight.new(ZcashNetwork.Testnet, 280_000),
                    1991772603L,
                    "76b809bb",
                    testnetBackend
                ),
                arrayOf(
                    "Blossom",
                    BlockHeight.new(ZcashNetwork.Testnet, 584_000),
                    733220448L,
                    "2bb40e60",
                    testnetBackend
                ),
                arrayOf(
                    "Heartwood",
                    BlockHeight.new(ZcashNetwork.Testnet, 903_800),
                    4122551051L,
                    "f5b9230b",
                    testnetBackend
                ),
                arrayOf(
                    "Canopy",
                    BlockHeight.new(ZcashNetwork.Testnet, 1_028_500),
                    3925833126L,
                    "e9ff75a6",
                    testnetBackend
                )
            )
        }
    }
}
