package cash.z.ecc.android.sdk.darkside.fixture

import cash.z.ecc.android.sdk.darkside.test.DarksideTestCoordinator
import cash.z.ecc.android.sdk.darkside.test.ScopedTest
import kotlinx.coroutines.CoroutineScope

object DarksideFixture {
    fun newDarksideTestCoordinator() =
        DarksideTestCoordinator().apply {
            enterTheDarkside()
        }

    const val blocksUrl =
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/basic-reorg/before-reorg.txt"
    val tx663174 = FixtureTransaction(
        663174,
        buildTxUrl("0821a89be7f2fc1311792c3fa1dd2171a8cdfb2effd98590cbd5ebcdcfcf491f")
    )
    val tx663188 = FixtureTransaction(
        663188,
        buildTxUrl("15a677b6770c5505fb47439361d3d3a7c21238ee1a6874fdedad18ae96850590")
    )

    private fun buildTxUrl(tx: String) =
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/$tx.txt"
}

