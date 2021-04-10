package cash.z.ecc.android.sdk.jni

import androidx.test.ext.junit.runners.AndroidJUnit4
import cash.z.ecc.android.bip39.Mnemonics.MnemonicCode
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.ext.TroubleshootingTwig
import cash.z.ecc.android.sdk.ext.Twig
import cash.z.ecc.android.sdk.test.BuildConfig
import cash.z.ecc.android.sdk.tool.DerivationTool
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransparentTest {

    lateinit var expected: Expected
    lateinit var network: ZcashNetwork

    @Before
    fun setup() {
        if (BuildConfig.FLAVOR == "zcashtestnet") {
            expected = ExpectedTestnet
            network = ZcashNetwork.Testnet
        } else {
            expected = ExpectedMainnet
            network = ZcashNetwork.Mainnet
        }
    }

    @Test
    fun deriveTransparentSecretKeyTest() {
        assertEquals(expected.tskCompressed, DerivationTool.deriveTransparentSecretKey(SEED, network = network))
    }

    @Test
    fun deriveTransparentAddressTest() {
        assertEquals(expected.tAddr, DerivationTool.deriveTransparentAddress(SEED, network = network))
    }

    @Test
    fun deriveTransparentAddressFromSecretKeyTest() {
        assertEquals(expected.tAddr, DerivationTool.deriveTransparentAddressFromPrivateKey(expected.tskCompressed, network = network))
    }

    @Test
    fun deriveUnifiedViewingKeysFromSeedTest() {
        val uvks = DerivationTool.deriveUnifiedViewingKeys(SEED, network = network)
        assertEquals(1, uvks.size)
        val uvk = uvks.first()
        assertEquals(expected.zAddr, DerivationTool.deriveShieldedAddress(uvk.extfvk, network = network))
        assertEquals(expected.tAddr, DerivationTool.deriveTransparentAddressFromPublicKey(uvk.extpub, network = network))
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

        object ExpectedMainnet : Expected {
            override val tAddr = "t1PKtYdJJHhc3Pxowmznkg7vdTwnhEsCvR4"
            override val zAddr = "zs1yc4sgtfwwzz6xfsy2xsradzr6m4aypgxhfw2vcn3hatrh5ryqsr08sgpemlg39vdh9kfupx20py"
            override val tskCompressed = "L4BvDC33yLjMRxipZvdiUmdYeRfZmR8viziwsVwe72zJdGbiJPv2"
            override val tpk = "03b1d7fb28d17c125b504d06b1530097e0a3c76ada184237e3bc0925041230a5af"
        }

        object ExpectedTestnet : Expected {
            override val tAddr = "tm9v3KTsjXK8XWSqiwFjic6Vda6eHY9Mjjq"
            override val zAddr = "ztestsapling1wn3tw9w5rs55x5yl586gtk72e8hcfdq8zsnjzcu8p7ghm8lrx54axc74mvm335q7lmy3g0sqje6"
            override val tskCompressed = "KzVugoXxR7AtTMdR5sdJtHxCNvMzQ4H196k7ATv4nnjoummsRC9G"
            override val tpk = "03b1d7fb28d17c125b504d06b1530097e0a3c76ada184237e3bc0925041230a5af"
        }

        @BeforeClass
        @JvmStatic
        fun startup() {
            Twig.plant(TroubleshootingTwig(formatter = { "@TWIG $it" }))
        }
    }

    interface Expected {
        val tAddr: String
        val zAddr: String
        val tskCompressed: String
        val tpk: String
    }
}
