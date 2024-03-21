package cash.z.ecc.android.sdk.jni

import androidx.test.filters.SmallTest
import cash.z.ecc.android.bip39.Mnemonics.MnemonicCode
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.annotation.MaintainedTest
import cash.z.ecc.android.sdk.annotation.TestPurpose
import cash.z.ecc.android.sdk.internal.Derivation
import cash.z.ecc.android.sdk.internal.deriveUnifiedAddress
import cash.z.ecc.android.sdk.internal.deriveUnifiedFullViewingKeysTypesafe
import cash.z.ecc.android.sdk.internal.jni.RustDerivationTool
import cash.z.ecc.android.sdk.model.ZcashNetwork
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
    fun deriveUnifiedFullViewingKeysFromSeedTest() =
        runBlocking {
            val ufvks =
                RustDerivationTool.new().deriveUnifiedFullViewingKeysTypesafe(
                    SEED,
                    network = network,
                    numberOfAccounts =
                        Derivation.DEFAULT_NUMBER_OF_ACCOUNTS
                )
            assertEquals(1, ufvks.size)
            val ufvk = ufvks.first()
            assertEquals(
                expected.uAddr,
                RustDerivationTool.new().deriveUnifiedAddress(ufvk.encoding, network = network)
            )
            // TODO: If we need this, change DerivationTool to derive from the UFVK instead of the public key.
            // assertEquals(expected.tAddr, DerivationTool.deriveTransparentAddressFromPublicKey(ufvk.encoding,
            //     network = network))
        }

    companion object {
        const val PHRASE =
            "deputy visa gentle among clean scout farm drive comfort patch skin salt ranch cool ramp" +
                " warrior drink narrow normal lunch behind salt deal person"
        val MNEMONIC = MnemonicCode(PHRASE)
        val SEED = MNEMONIC.toSeed()

        @Suppress("MaxLineLength", "ktlint:standard:max-line-length")
        object ExpectedMainnet : Expected {
            override val tAddr = "t1PKtYdJJHhc3Pxowmznkg7vdTwnhEsCvR4"
            override val zAddr = "zs1yc4sgtfwwzz6xfsy2xsradzr6m4aypgxhfw2vcn3hatrh5ryqsr08sgpemlg39vdh9kfupx20py"
            override val uAddr = "u1t23erzgkn7c6c2jn66rspl4m45lg8rn3f7mn7le4yxk7693wr7sgx472jn95s00x8kx3hct5ej4tf76k59dfhsd809t7mzt9ldzw8f5083fw4xqvxfshl9u7ed2wyv6ypmzny0px0nvszslr5kr7fgk2zgfnlycddzqak4adsqjdzp76y7fl0k4ygamjr43t6rpxsf6xql8g20rdk0h"
            override val tAccountPrivKey = "xprv9z1aorRbyM5A6ok9QmdCUztMRRgthiNpus4u8Rgn9YeZEz1EVkLthFpJS1Y1FaXAvgNDPKTwxvshUMj7KJiGeNVhKL8RzDv14yHbUu3szy5"
            override val tskCompressed = "L4BvDC33yLjMRxipZvdiUmdYeRfZmR8viziwsVwe72zJdGbiJPv2"
            override val tpk = "03b1d7fb28d17c125b504d06b1530097e0a3c76ada184237e3bc0925041230a5af"
        }

        @Suppress("MaxLineLength", "ktlint:standard:max-line-length")
        object ExpectedTestnet : Expected {
            override val tAddr = "tm9v3KTsjXK8XWSqiwFjic6Vda6eHY9Mjjq"
            override val zAddr = "ztestsapling1wn3tw9w5rs55x5yl586gtk72e8hcfdq8zsnjzcu8p7ghm8lrx54axc74mvm335q7lmy3g0sqje6"
            override val uAddr = "utest10prpna6ydq6042q7t9qqr66qg9xhj8gn7aesrxnxelp7sqh5lc56w9qzl4pydhjvt9v34cxp5krdracecwg3dpkvgv8fttvz4hcql2m35se6u2n8h9p86xc7c6wm8fj7p4r3kq0mvvl4g650s6xdkhkg9yhtnnne4vy9k3hw27m0y6ctlmgkeadvn38v6wp9fpdwwhwrgn52z8fp6pc"
            override val tAccountPrivKey = "xprv9yUDoMsKVAQ8W8tf3VuPGyBKHuDPa4SkBXT7KHp4dfW7iBWKUEgAYG1g6ZpdotTWc4iMrj6vgaT8otHCWRj5SYtXkDcxkheFCp6QZEW9dPi"
            override val tskCompressed = "KzVugoXxR7AtTMdR5sdJtHxCNvMzQ4H196k7ATv4nnjoummsRC9G"
            override val tpk = "03b1d7fb28d17c125b504d06b1530097e0a3c76ada184237e3bc0925041230a5af"
        }

        @BeforeClass
        @JvmStatic
        fun startup() {
        }

        @JvmStatic
        @Parameterized.Parameters
        fun data() =
            listOf(
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
