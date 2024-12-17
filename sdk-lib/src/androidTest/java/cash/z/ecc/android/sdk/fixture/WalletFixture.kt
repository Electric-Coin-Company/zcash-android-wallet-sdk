package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.sdk.internal.jni.RustDerivationTool
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.ZcashNetwork

object WalletFixture {
    const val ACCOUNT_INDEX = 0L

    val NETWORK = ZcashNetwork.Mainnet

    // This is the "Ben" wallet phrase from sdk-incubator-lib.
    const val BEN_SEED_PHRASE =
        "kitchen renew wide common vague fold vacuum tilt amazing pear square gossip jewel month" +
            " tree shock scan alpha just spot fluid toilet view dinner"

    // This is the "Alice" wallet phrase from sdk-incubator-lib.
    const val ALICE_SEED_PHRASE =
        "wish puppy smile loan doll curve hole maze file ginger hair nose key relax knife witness" +
            " cannon grab despair throw review deal slush frame"

    suspend fun getUnifiedSpendingKey(
        seed: String = BEN_SEED_PHRASE,
        network: ZcashNetwork = NETWORK,
        accountIndex: Long = ACCOUNT_INDEX
    ) = UnifiedSpendingKey.new(
        RustDerivationTool.new().deriveUnifiedSpendingKey(
            Mnemonics.MnemonicCode(seed).toEntropy(),
            network.id,
            accountIndex
        )
    )
}
