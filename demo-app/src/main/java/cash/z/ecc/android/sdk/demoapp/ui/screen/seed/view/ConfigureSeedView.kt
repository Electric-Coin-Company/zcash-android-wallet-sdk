package cash.z.ecc.android.sdk.demoapp.ui.screen.seed.view

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.demoapp.fixture.WalletFixture
import cash.z.ecc.android.sdk.demoapp.model.PersistableWallet
import cash.z.ecc.android.sdk.demoapp.model.SeedPhrase
import cash.z.ecc.android.sdk.model.ZcashNetwork

@Preview
@Composable
fun ComposablePreview() {
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
    Column {
        Text(text = stringResource(R.string.configure_seed))
        Button(
            onClick = {
                val newWallet = PersistableWallet(zcashNetwork, null, SeedPhrase.new(WalletFixture.Alice.seedPhrase))
                onExistingWallet(newWallet)
            }
        ) {
            Text(text = stringResource(id = R.string.person_alyssa))
        }
        Button(
            onClick = {
                val newWallet = PersistableWallet(zcashNetwork, null, SeedPhrase.new(WalletFixture.Alice.seedPhrase))
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
