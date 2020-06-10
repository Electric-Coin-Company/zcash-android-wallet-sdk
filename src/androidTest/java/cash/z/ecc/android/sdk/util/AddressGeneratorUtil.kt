package cash.z.ecc.android.sdk.util

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Initializer.WalletBirthday
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okio.Okio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@ExperimentalCoroutinesApi
class AddressGeneratorUtil {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val initializer = Initializer(context).open(WalletBirthday())
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
    fun generateAddresses() = runBlocking {
        readLines()
            .map { seedPhrase ->
                mnemonics.toSeed(seedPhrase.toCharArray())
            }.map { seed ->
                initializer.rustBackend.deriveAddress(seed)
            }.collect { address ->
                println("xrxrx2\t$address")
                assertTrue(address.startsWith("zs1"))
            }
    }

    @Throws(IOException::class)
    fun readLines() = flow<String> {
        val seedFile = javaClass.getResourceAsStream("/utils/seeds.txt")
        Okio.buffer(Okio.source(seedFile)).use { source ->
            var line: String? = source.readUtf8Line()
            while (line != null) {
                emit(line)
                line = source.readUtf8Line()
            }
        }
    }

}
