package cash.z.ecc.android.sdk.demoapp.ui.screen.seed.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cash.z.ecc.android.sdk.WalletInitMode
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.fixture.WalletFixture
import cash.z.ecc.android.sdk.model.PersistableWallet
import cash.z.ecc.android.sdk.model.SeedPhrase
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.model.defaultForNetwork
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint

@Preview(name = "Seed")
@Composable
private fun ComposablePreview() {
    MaterialTheme {
        Seed(
            ZcashNetwork.Mainnet,
            onExistingWallet = {},
            onNewWallet = {}
        )
    }
}

@Composable
fun Seed(
    zcashNetwork: ZcashNetwork,
    onExistingWallet: (PersistableWallet) -> Unit,
    onNewWallet: () -> Unit
) {
    Scaffold(topBar = {
        ConfigureSeedTopAppBar()
    }) { paddingValues ->
        ConfigureSeedMainContent(
            paddingValues = paddingValues,
            zcashNetwork = zcashNetwork,
            onExistingWallet = onExistingWallet,
            onNewWallet = onNewWallet
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ConfigureSeedTopAppBar() {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.configure_seed)) }
    )
}

@Composable
private fun ConfigureSeedMainContent(
    paddingValues: PaddingValues,
    zcashNetwork: ZcashNetwork,
    onExistingWallet: (PersistableWallet) -> Unit,
    onNewWallet: () -> Unit
) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        Button(
            onClick = {
                val newWallet = PersistableWallet(
                    network = zcashNetwork,
                    endpoint = LightWalletEndpoint.defaultForNetwork(zcashNetwork),
                    birthday = WalletFixture.Alice.getBirthday(zcashNetwork),
                    seedPhrase = SeedPhrase.new(WalletFixture.Alice.seedPhrase),
                    walletInitMode = WalletInitMode.RestoreWallet
                )
                onExistingWallet(newWallet)
            }
        ) {
            Text(text = stringResource(id = R.string.person_alyssa))
        }
        Button(
            onClick = {
                val newWallet = PersistableWallet(
                    network = zcashNetwork,
                    endpoint = LightWalletEndpoint.defaultForNetwork(zcashNetwork),
                    birthday = WalletFixture.Ben.getBirthday(zcashNetwork),
                    seedPhrase = SeedPhrase.new(WalletFixture.Ben.seedPhrase),
                    walletInitMode = WalletInitMode.RestoreWallet
                )
                onExistingWallet(newWallet)
            }
        ) {
            Text(text = stringResource(R.string.person_ben))
        }
        Button(
            onClick = onNewWallet
        ) {
            Text(text = stringResource(id = R.string.seed_random))
        }
    }
}
