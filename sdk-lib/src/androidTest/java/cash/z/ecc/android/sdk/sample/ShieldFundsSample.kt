package cash.z.ecc.android.sdk.sample

import cash.z.ecc.android.sdk.ext.Twig
import cash.z.ecc.android.sdk.type.NetworkType
import cash.z.ecc.android.sdk.type.ZcashNetwork
import cash.z.ecc.android.sdk.util.TestWallet
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

/**
 * Samples related to shielding funds.
 */
class ShieldFundsSample {

    val SEED_PHRASE = "still champion voice habit trend flight survey between bitter process artefact blind carbon truly provide dizzy crush flush breeze blouse charge solid fish spread" // \"//\"deputy visa gentle among clean scout farm drive comfort patch skin salt ranch cool ramp warrior drink narrow normal lunch behind salt deal person"//"deputy visa gentle among clean scout farm drive comfort patch skin salt ranch cool ramp warrior drink narrow normal lunch behind salt deal person"

    /**
     * This test will construct a t2z transaction. It is safe to run this repeatedly, because
     * nothing is submitted to the network (because the keys don't match the address so the encoding
     * fails). Originally, it's intent is just to exercise the code and troubleshoot any issues but
     * then it became clear that this would be a cool Sample Test and PoC for writing a SimpleWallet
     * class.
     */
    @Test
    @Ignore("This test is broken")
    fun constructT2Z() = runBlocking {
        Twig.sprout("ShieldFundsSample")

        val wallet = TestWallet(TestWallet.Backups.DEV_WALLET, NetworkType.Mainnet)

        Assert.assertEquals("foo", "${wallet.shieldedAddress} ${wallet.transparentAddress}")
//        wallet.shieldFunds()

        Twig.clip("ShieldFundsSample")
        Assert.assertEquals(5, wallet.synchronizer.saplingBalances.value.availableZatoshi)
    }
}
