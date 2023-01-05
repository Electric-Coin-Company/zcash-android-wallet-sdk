package cash.z.ecc.android.sdk.demoapp.ui.screen.home.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.demoapp.fixture.WalletSnapshotFixture
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel.WalletSnapshot

@Preview
@Composable
fun ComposablePreviewHome() {
    MaterialTheme {
        Home(
            WalletSnapshotFixture.new(),
            goBalance = {},
            goSend = {},
            goAddressDetails = {},
            isTestnet = true,
            goTestnetFaucet = {},
            resetSdk = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
@Composable
fun Home(
    walletSnapshot: WalletSnapshot,
    isTestnet: Boolean,
    goBalance: () -> Unit,
    goSend: () -> Unit,
    goAddressDetails: () -> Unit,
    goTestnetFaucet: () -> Unit,
    resetSdk: () -> Unit,
) {
    Scaffold(topBar = {
        HomeTopAppBar(isTestnet, goTestnetFaucet, resetSdk)
    }) { paddingValues ->
        HomeMainContent(
            paddingValues = paddingValues,
            walletSnapshot,
            goBalance = goBalance,
            goSend = goSend,
            goAddressDetails = goAddressDetails
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeTopAppBar(
    isTestnet: Boolean,
    goTestnetFaucet: () -> Unit,
    resetSdk: () -> Unit
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.app_name)) },
        actions = {
            DebugMenu(
                isTestnet,
                goTestnetFaucet = goTestnetFaucet,
                resetSdk = resetSdk
            )
        }
    )
}

@Composable
private fun DebugMenu(
    isTestnet: Boolean,
    goTestnetFaucet: () -> Unit,
    resetSdk: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = null)
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        if (isTestnet) {
            DropdownMenuItem(
                text = { Text("Open Testnet Faucet") },
                onClick = {
                    goTestnetFaucet()
                    expanded = false
                }
            )
        }
        DropdownMenuItem(
            text = { Text("Reset SDK") },
            onClick = {
                resetSdk()
                expanded = false
            }
        )
    }
}

@Composable
private fun HomeMainContent(
    paddingValues: PaddingValues,
    walletSnapshot: WalletSnapshot,
    goBalance: () -> Unit,
    goSend: () -> Unit,
    goAddressDetails: () -> Unit
) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        Button(goBalance) {
            Text(text = stringResource(id = R.string.menu_balance))
        }

        Button(goSend) {
            Text(text = stringResource(id = R.string.menu_send))
        }

        Button(goAddressDetails) {
            Text(text = stringResource(id = R.string.menu_address))
        }

        Text(text = stringResource(id = R.string.home_status, walletSnapshot.status.toString()))
        if (walletSnapshot.status != Synchronizer.Status.SYNCED) {
            @Suppress("MagicNumber")
            Text(text = stringResource(id = R.string.home_progress, walletSnapshot.progress.decimal * 100))
        }
    }
}
