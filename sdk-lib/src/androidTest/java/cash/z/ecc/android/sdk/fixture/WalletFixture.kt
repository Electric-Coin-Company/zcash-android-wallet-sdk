package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.sdk.internal.deriveUnifiedSpendingKey
import cash.z.ecc.android.sdk.internal.jni.RustDerivationTool
import cash.z.ecc.android.sdk.model.ZcashNetwork

object WalletFixture {
    // TODO [#1644]: Refactor Account ZIP32 index across SDK
    // TODO [#1644]: https://github.com/Electric-Coin-Company/zcash-android-wallet-sdk/issues/1644
    const val ACCOUNT_INDEX = 0

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
        accountIndex: Int = ACCOUNT_INDEX
    ) = RustDerivationTool.new().deriveUnifiedSpendingKey(
        Mnemonics.MnemonicCode(seed).toEntropy(),
        network,
        accountIndex
    )
}
