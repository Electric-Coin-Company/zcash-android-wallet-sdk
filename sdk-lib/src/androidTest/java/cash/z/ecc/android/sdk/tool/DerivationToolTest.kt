package cash.z.ecc.android.sdk.tool

import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.sdk.fixture.WalletFixture
import cash.z.ecc.android.sdk.internal.deriveUnifiedSpendingKey
import cash.z.ecc.android.sdk.internal.jni.RustDerivationTool
import cash.z.ecc.android.sdk.model.Account
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertContentEquals

class DerivationToolTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun create_spending_key_does_not_mutate_passed_bytes() = runTest {
        val bytesOne = Mnemonics.MnemonicCode(WalletFixture.SEED_PHRASE).toEntropy()
        val bytesTwo = Mnemonics.MnemonicCode(WalletFixture.SEED_PHRASE).toEntropy()

        RustDerivationTool.deriveUnifiedSpendingKey(bytesOne, WalletFixture.NETWORK, Account.DEFAULT)

        assertContentEquals(bytesTwo, bytesOne)
    }
}
