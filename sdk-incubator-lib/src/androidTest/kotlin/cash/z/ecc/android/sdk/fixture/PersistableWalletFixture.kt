package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.WalletInitMode
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.PersistableWallet
import cash.z.ecc.android.sdk.model.SeedPhrase
import cash.z.ecc.android.sdk.model.Testnet
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint

object PersistableWalletFixture {

    val NETWORK = ZcashNetwork.Testnet

    val ENDPOINT = LightWalletEndpoint.Testnet

    // These came from the mainnet 1500000.json file
    @Suppress("MagicNumber")
    val BIRTHDAY = BlockHeight.new(ZcashNetwork.Mainnet, 1500000L)

    val SEED_PHRASE = SeedPhraseFixture.new()

    val WALLET_INIT_MODE = WalletInitMode.ExistingWallet

    fun new(
        network: ZcashNetwork = NETWORK,
        endpoint: LightWalletEndpoint = ENDPOINT,
        birthday: BlockHeight = BIRTHDAY,
        seedPhrase: SeedPhrase = SEED_PHRASE,
        walletInitMode: WalletInitMode = WALLET_INIT_MODE
    ) = PersistableWallet(network, endpoint, birthday, seedPhrase, walletInitMode)
}
