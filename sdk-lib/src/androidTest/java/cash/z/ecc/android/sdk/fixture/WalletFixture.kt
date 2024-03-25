package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.sdk.internal.deriveUnifiedSpendingKey
import cash.z.ecc.android.sdk.internal.jni.RustDerivationTool
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.ZcashNetwork

object WalletFixture {
    val NETWORK = ZcashNetwork.Mainnet

    // This is the "Ben" wallet phrase from sdk-incubator-lib.
    const val SEED_PHRASE =
        "kitchen renew wide common vague fold vacuum tilt amazing pear square gossip jewel month" +
            " tree shock scan alpha just spot fluid toilet view dinner"

    // This is the "Alice" wallet phrase from sdk-incubator-lib.
    const val ALICE_SEED_PHRASE =
        "wish puppy smile loan doll curve hole maze file ginger hair nose key relax knife witness" +
            " cannon grab despair throw review deal slush frame"

    suspend fun getUnifiedSpendingKey(
        seed: String = SEED_PHRASE,
        network: ZcashNetwork = NETWORK,
        account: Account = Account.DEFAULT
    ) = RustDerivationTool.new().deriveUnifiedSpendingKey(Mnemonics.MnemonicCode(seed).toEntropy(), network, account)
}
