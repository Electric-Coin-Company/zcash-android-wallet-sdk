package cash.z.ecc.android.sdk.demoapp.ui.screen.keys.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.demoapp.fixture.WalletSnapshotFixture
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel.SendState
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel.WalletSnapshot
import cash.z.ecc.android.sdk.ext.convertZatoshiToZec
import cash.z.ecc.android.sdk.ext.toUsdString
import cash.z.ecc.android.sdk.model.toZecString

@Preview(name = "Keys")
@Composable
private fun ComposablePreview() {
    MaterialTheme {
        Keys(
            keysState = KeysState(),
            onBack = {},
        )
    }
}

@Composable
fun Keys(
    keysState: KeysState,
    onBack: () -> Unit,
) {
    Scaffold(topBar = {
        KeysTopAppBar(
            onBack,
        )
    }) { paddingValues ->
        KeysMainContent(
            paddingValues = paddingValues,
            keysState = keysState,
        )
    }
}

@Composable
private fun KeysTopAppBar(
    onBack: () -> Unit,
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.menu_keys)) },
        navigationIcon = {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
private fun KeysMainContent(
    paddingValues: PaddingValues,
    keysState: KeysState,
) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        Text(stringResource(id = R.string.balance_orchard))
        Text(
            stringResource(
                id = R.string.balance_available_amount_format,
                walletSnapshot.orchardBalance.available.toZecString(),
                walletSnapshot.exchangeRateUsd?.multiply(walletSnapshot.orchardBalance.available.convertZatoshiToZec())
                    .toUsdString()
            )
        )
        Text(
            stringResource(
                id = R.string.balance_pending_amount_format,
                walletSnapshot.orchardBalance.pending.toZecString(),
                walletSnapshot.exchangeRateUsd?.multiply(walletSnapshot.orchardBalance.pending.convertZatoshiToZec())
                    .toUsdString()
            )
        )

        Spacer(Modifier.padding(8.dp))

        Text(stringResource(id = R.string.balance_sapling))
        Text(
            stringResource(
                id = R.string.balance_available_amount_format,
                walletSnapshot.saplingBalance.available.toZecString(),
                walletSnapshot.exchangeRateUsd?.multiply(walletSnapshot.saplingBalance.available.convertZatoshiToZec())
                    .toUsdString()
            )
        )
        Text(
            stringResource(
                id = R.string.balance_pending_amount_format,
                walletSnapshot.saplingBalance.pending.toZecString(),
                walletSnapshot.exchangeRateUsd?.multiply(walletSnapshot.saplingBalance.pending.convertZatoshiToZec())
                    .toUsdString()
            )
        )

        Spacer(Modifier.padding(8.dp))

        Text(stringResource(id = R.string.balance_transparent))
        Text(
            stringResource(
                id = R.string.balance_available_amount_format,
                walletSnapshot.transparentBalance.toZecString(),
                walletSnapshot.exchangeRateUsd?.multiply(walletSnapshot.transparentBalance.convertZatoshiToZec())
                    .toUsdString()
            )
        )

        // This check is not entirely correct - it does not calculate the resulting fee with the new Proposal API
        if (walletSnapshot.transparentBalance.value > 0L) {
            // Note this implementation does not guard against multiple clicks
            Button(onClick = onShieldFunds) {
                Text(stringResource(id = R.string.action_shield))
            }
        }

        // Eventually there should be something to clear the status
        Text(stringResource(id = R.string.send_status, sendState.toString()))
    }
}
