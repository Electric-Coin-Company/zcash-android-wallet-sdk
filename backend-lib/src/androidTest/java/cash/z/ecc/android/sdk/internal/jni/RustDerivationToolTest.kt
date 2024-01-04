package cash.z.ecc.android.sdk.internal.jni

import cash.z.ecc.android.bip39.Mnemonics
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertContentEquals

class RustDerivationToolTest {
    companion object {
        private const val SEED_PHRASE =
            "kitchen renew wide common vague fold vacuum tilt amazing pear square gossip jewel month tree shock scan" +
                " alpha just spot fluid toilet view dinner"
    }

    @Test
    fun create_spending_key_does_not_mutate_passed_bytes() =
        runTest {
            val bytesOne = Mnemonics.MnemonicCode(SEED_PHRASE).toEntropy()
            val bytesTwo = Mnemonics.MnemonicCode(SEED_PHRASE).toEntropy()

            RustDerivationTool.new().deriveUnifiedSpendingKey(bytesOne, networkId = 1, accountIndex = 0)

            assertContentEquals(bytesTwo, bytesOne)
        }
}
