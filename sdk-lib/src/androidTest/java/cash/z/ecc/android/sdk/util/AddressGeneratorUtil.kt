package cash.z.ecc.android.sdk.util

import cash.z.ecc.android.sdk.fixture.AccountFixture
import cash.z.ecc.android.sdk.internal.deriveUnifiedAddress
import cash.z.ecc.android.sdk.internal.jni.RustDerivationTool
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.test.readFileLinesInFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@ExperimentalCoroutinesApi
class AddressGeneratorUtil {
    private val mnemonics = SimpleMnemonics()

    @Test
    fun printMnemonic() {
        mnemonics.apply {
            val mnemonicPhrase = String(nextMnemonic())
            println("example mnemonic: $mnemonicPhrase")
            assertEquals(24, mnemonicPhrase.split(" ").size)
        }
    }

    @Test
    fun generateAddresses() =
        runBlocking {
            readFileLinesInFlow("/utils/seeds.txt")
                .map { seedPhrase ->
                    mnemonics.toSeed(seedPhrase.toCharArray())
                }.map { seed ->
                    RustDerivationTool.new().deriveUnifiedAddress(
                        seed = seed,
                        network = ZcashNetwork.Mainnet,
                        accountIndex = AccountFixture.new().hdAccountIndex!!
                    )
                }.collect { address ->
                    println("xrxrx2\t$address")
                    assertTrue(address.startsWith("u1"))
                }
        }
}
