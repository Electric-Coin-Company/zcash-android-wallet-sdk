package cash.z.ecc.android.sdk.jni

import androidx.test.ext.junit.runners.AndroidJUnit4
import cash.z.ecc.android.bip39.Mnemonics.MnemonicCode
import cash.z.ecc.android.bip39.Mnemonics.WordCount.COUNT_24
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.ext.TroubleshootingTwig
import cash.z.ecc.android.sdk.ext.Twig
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.tool.DerivationTool
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransparentTest {

    @Before
    fun setup() {

    }

    @Test
    fun deriveTransparentSecretKeyTest() {
        assertEquals(Expected.tskCompressed, DerivationTool.deriveTransparentSecretKey(SEED))
    }

    @Test
    fun deriveTransparentAddressTest() {
        assertEquals(Expected.tAddr, DerivationTool.deriveTransparentAddress(SEED))
    }

    @Test
    fun deriveTransparentAddressFromSecretKeyTest() {
        assertEquals(Expected.tAddr, DerivationTool.deriveTransparentAddress(Expected.tskCompressed))
    }

//    @Test
//    fun deriveTransparentAddressFromSecretKeyTest2() {
//        while(false) {
//            MnemonicCode(COUNT_24).let { phrase ->
//                val addr = DerivationTool.deriveShieldedAddress(phrase.toSeed())
//                twig("$addr${String(phrase.chars)}\t")
//            }
//        }
//    }


    companion object {
        const val PHRASE = "deputy visa gentle among clean scout farm drive comfort patch skin salt ranch cool ramp warrior drink narrow normal lunch behind salt deal person"
        val MNEMONIC = MnemonicCode(PHRASE)
        val SEED = MNEMONIC.toSeed()

        object Expected {
            val tAddr = "t1PKtYdJJHhc3Pxowmznkg7vdTwnhEsCvR4"

            // private key in compressed Wallet Import Format (WIF)
            val tskCompressed = "L4BvDC33yLjMRxipZvdiUmdYeRfZmR8viziwsVwe72zJdGbiJPv2"
        }

        @BeforeClass
        @JvmStatic
        fun startup() {
            Twig.plant(TroubleshootingTwig(formatter = {"@TWIG $it"}))
        }
    }
}