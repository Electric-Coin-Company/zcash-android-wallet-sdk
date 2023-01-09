package cash.z.ecc.android.sdk.demoapp.ui.screen.addresses.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.demoapp.model.WalletAddresses
import kotlinx.coroutines.flow.flow

// @Preview
// @Composable
// fun ComposablePreview() {
//     MaterialTheme {
//         Addresses()
//     }
// }

/**
 * @param copyToClipboard First string is a tag, the second string is the text to copy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Addresses(
    synchronizer: Synchronizer,
    copyToClipboard: (String, String) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        topBar = { AddressesTopAppBar(onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // TODO [#846]: Slow addresses providing
        // TODO [#846]: https://github.com/zcash/zcash-android-wallet-sdk/issues/846
        val walletAddresses = flow {
            emit(WalletAddresses.new(synchronizer))
        }.collectAsState(
            initial = null
        ).value
        if (null != walletAddresses) {
            AddressesMainContent(
                paddingValues = paddingValues,
                addresses = walletAddresses,
                copyToClipboard = copyToClipboard
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AddressesTopAppBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.menu_address)) },
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
private fun AddressesMainContent(
    paddingValues: PaddingValues,
    addresses: WalletAddresses,
    copyToClipboard: (String, String) -> Unit
) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        Text(stringResource(id = R.string.unified_address))
        addresses.unified.address.also { address ->
            val tag = stringResource(id = R.string.unified_address)

            Text(
                address,
                Modifier.clickable {
                    copyToClipboard(tag, address)
                }
            )
        }

        Spacer(Modifier.padding(8.dp))

        Text(stringResource(id = R.string.sapling_address))
        addresses.sapling.address.also { address ->
            val tag = stringResource(id = R.string.sapling_address_tag)

            Text(
                address,
                Modifier.clickable {
                    copyToClipboard(tag, address)
                }
            )
        }

        Spacer(Modifier.padding(8.dp))

        Text(stringResource(id = R.string.transparent_address))
        addresses.transparent.address.also { address ->
            val tag = stringResource(id = R.string.transparent_address)

            Text(
                address,
                Modifier.clickable {
                    copyToClipboard(tag, address)
                }
            )
        }
    }
}
