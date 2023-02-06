package cash.z.ecc.android.sdk.demoapp.ui.screen.balance.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel.WalletSnapshot
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.model.toZecString

// @Preview
// @Composable
// fun ComposablePreview() {
//     MaterialTheme {
//         Addresses()
//     }
// }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Balance(
    walletSnapshot: WalletSnapshot,
    onShieldFunds: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(topBar = {
        BalanceTopAppBar(onBack)
    }) { paddingValues ->

        BalanceMainContent(
            paddingValues = paddingValues,
            walletSnapshot,
            onShieldFunds = onShieldFunds
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BalanceTopAppBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.menu_balance)) },
        navigationIcon = {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
private fun BalanceMainContent(
    paddingValues: PaddingValues,
    walletSnapshot: WalletSnapshot,
    onShieldFunds: () -> Unit
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
                walletSnapshot.orchardBalance.available.toZecString()
            )
        )
        Text(
            stringResource(
                id = R.string.balance_pending_amount_format,
                walletSnapshot.orchardBalance.pending.toZecString()
            )
        )

        Spacer(Modifier.padding(8.dp))

        Text(stringResource(id = R.string.balance_sapling))
        Text(
            stringResource(
                id = R.string.balance_available_amount_format,
                walletSnapshot.saplingBalance.available.toZecString()
            )
        )
        Text(
            stringResource(
                id = R.string.balance_pending_amount_format,
                walletSnapshot.saplingBalance.pending.toZecString()
            )
        )

        Spacer(Modifier.padding(8.dp))

        Text(stringResource(id = R.string.balance_transparent))
        Text(
            stringResource(
                id = R.string.balance_available_amount_format,
                walletSnapshot.transparentBalance.available.toZecString()
            )
        )
        Text(
            stringResource(
                id = R.string.balance_pending_amount_format,
                walletSnapshot.transparentBalance.pending.toZecString()
            )
        )

        // This check will not be correct with variable fees
        if (walletSnapshot.transparentBalance.available > ZcashSdk.MINERS_FEE) {
            // Note this implementation does not guard against multiple clicks
            Button(onClick = onShieldFunds) {
                Text(stringResource(id = R.string.action_shield))
            }
        }
    }
}
