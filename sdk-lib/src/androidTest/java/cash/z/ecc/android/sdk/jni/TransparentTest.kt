package cash.z.ecc.android.sdk.jni

import androidx.test.filters.SmallTest
import cash.z.ecc.android.bip39.Mnemonics.MnemonicCode
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.annotation.MaintainedTest
import cash.z.ecc.android.sdk.annotation.TestPurpose
import cash.z.ecc.android.sdk.internal.TroubleshootingTwig
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.DerivationTool
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MaintainedTest(TestPurpose.REGRESSION)
@RunWith(Parameterized::class)
@SmallTest
class TransparentTest(val expected: Expected, val network: ZcashNetwork) {

    @Test
    fun deriveTransparentAccountPrivateKeyTest() = runBlocking {
        assertEquals(expected.tAccountPrivKey, DerivationTool.deriveTransparentAccountPrivateKey(SEED, network = network))
    }

    @Test
    fun deriveTransparentAddressTest() = runBlocking {
        assertEquals(expected.tAddr, DerivationTool.deriveTransparentAddress(SEED, network = network))
    }

    @Test
    fun deriveTransparentAddressFromAccountPrivateKeyTest() = runBlocking {
        val pk = DerivationTool.deriveTransparentAccountPrivateKey(SEED, network = network)
        assertEquals(expected.tAddr, DerivationTool.deriveTransparentAddressFromAccountPrivateKey(pk, network = network))
    }

    @Test
    fun deriveUnifiedFullViewingKeysFromSeedTest() = runBlocking {
        val ufvks = DerivationTool.deriveUnifiedFullViewingKeys(SEED, network = network)
        assertEquals(1, ufvks.size)
        val ufvk = ufvks.first()
        assertEquals(expected.uAddr, DerivationTool.deriveUnifiedAddress(ufvk.encoding, network = network))
        // TODO: If we need this, change DerivationTool to derive from the UFVK instead of the public key.
        // assertEquals(expected.tAddr, DerivationTool.deriveTransparentAddressFromPublicKey(ufvk.encoding,
        //     network = network))
    }

    companion object {
        const val PHRASE = "deputy visa gentle among clean scout farm drive comfort patch skin salt ranch cool ramp warrior drink narrow normal lunch behind salt deal person"
        val MNEMONIC = MnemonicCode(PHRASE)
        val SEED = MNEMONIC.toSeed()

        object ExpectedMainnet : Expected {
            override val tAddr = "t1PKtYdJJHhc3Pxowmznkg7vdTwnhEsCvR4"
            override val zAddr = "zs1yc4sgtfwwzz6xfsy2xsradzr6m4aypgxhfw2vcn3hatrh5ryqsr08sgpemlg39vdh9kfupx20py"
            override val uAddr = "u1607xqhx72u8x94xcg6kyt9sd83aw8zvys2vwlr5n956e5jfytcaaeuzrk938c03jv4t0kdk73yxz9yd8rdksutw68ycpy6yt9vzhu28z58rh89gtt653cspr0c50ev4av0ddzj5vrrh"
            override val tAccountPrivKey = "xprv9z1aorRbyM5A6ok9QmdCUztMRRgthiNpus4u8Rgn9YeZEz1EVkLthFpJS1Y1FaXAvgNDPKTwxvshUMj7KJiGeNVhKL8RzDv14yHbUu3szy5"
            override val tskCompressed = "L4BvDC33yLjMRxipZvdiUmdYeRfZmR8viziwsVwe72zJdGbiJPv2"
            override val tpk = "03b1d7fb28d17c125b504d06b1530097e0a3c76ada184237e3bc0925041230a5af"
        }

        object ExpectedTestnet : Expected {
            override val tAddr = "tm9v3KTsjXK8XWSqiwFjic6Vda6eHY9Mjjq"
            override val zAddr = "ztestsapling1wn3tw9w5rs55x5yl586gtk72e8hcfdq8zsnjzcu8p7ghm8lrx54axc74mvm335q7lmy3g0sqje6"
            override val uAddr = "utest1cy80kzr6fj5vrrazldtcgmycs6rgu2x73pvwrjjmlwrwx343m06lxua5u36jdwyeckn4a6a0fkxm4y7t3lvhzscqrwg3gxpj4rgrgmf93m0cpm9ddkzn5qyzgadktuwza5d5kucewv3"
            override val tAccountPrivKey = "xprv9yUDoMsKVAQ8W8tf3VuPGyBKHuDPa4SkBXT7KHp4dfW7iBWKUEgAYG1g6ZpdotTWc4iMrj6vgaT8otHCWRj5SYtXkDcxkheFCp6QZEW9dPi"
            override val tskCompressed = "KzVugoXxR7AtTMdR5sdJtHxCNvMzQ4H196k7ATv4nnjoummsRC9G"
            override val tpk = "03b1d7fb28d17c125b504d06b1530097e0a3c76ada184237e3bc0925041230a5af"
        }

        @BeforeClass
        @JvmStatic
        fun startup() {
            Twig.plant(TroubleshootingTwig(formatter = { "@TWIG $it" }))
        }

        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf(ExpectedTestnet, ZcashNetwork.Testnet),
            arrayOf(ExpectedMainnet, ZcashNetwork.Mainnet)
        )
    }

    interface Expected {
        val tAddr: String
        val zAddr: String
        val uAddr: String
        val tAccountPrivKey: String
        val tskCompressed: String
        val tpk: String
    }
}
