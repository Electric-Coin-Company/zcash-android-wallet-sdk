package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.sdk.internal.deriveUnifiedSpendingKey
import cash.z.ecc.android.sdk.internal.jni.RustDerivationTool
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.ZcashNetwork

object WalletFixture {
    val NETWORK = ZcashNetwork.Mainnet
    const val SEED_PHRASE =
        "kitchen renew wide common vague fold vacuum tilt amazing pear square gossip jewel month tree shock scan alpha just spot fluid toilet view dinner"

    suspend fun getUnifiedSpendingKey(
        seed: String = SEED_PHRASE,
        network: ZcashNetwork = NETWORK,
        account: Account = Account.DEFAULT
    ) = RustDerivationTool.deriveUnifiedSpendingKey(Mnemonics.MnemonicCode(seed).toEntropy(), network, account)
}
