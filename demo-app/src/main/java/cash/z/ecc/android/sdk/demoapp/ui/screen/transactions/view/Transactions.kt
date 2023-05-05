package cash.z.ecc.android.sdk.demoapp.ui.screen.transactions.view

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.WalletAddresses
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

// @Preview
// @Composable
// fun ComposablePreview() {
//     MaterialTheme {
//         Addresses()
//     }
// }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Transactions(
    synchronizer: Synchronizer,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { TransactionsTopAppBar(onBack) }
    ) { paddingValues ->
        // TODO [#846]: Slow addresses providing
        // TODO [#846]: https://github.com/zcash/zcash-android-wallet-sdk/issues/846
        val walletAddresses = flow {
            emit(WalletAddresses.new(synchronizer))
        }.collectAsState(
            initial = null
        ).value
        if (null != walletAddresses) {
            TransactionsMainContent(
                paddingValues = paddingValues,
                synchronizer,
                synchronizer.transactions.collectAsStateWithLifecycle(initialValue = emptyList()).value
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TransactionsTopAppBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.menu_transactions)) },
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
private fun TransactionsMainContent(
    paddingValues: PaddingValues,
    synchronizer: Synchronizer,
    transactions: List<TransactionOverview>
) {
    val queryScope = rememberCoroutineScope()
    Column(
        Modifier
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            items(transactions) {
                Button({
                    val memos = synchronizer.getMemos(it)
                    queryScope.launch {
                        runCatching {
                            Log.v("Zcash", "Transaction memos: ${memos.toList()}")
                        }.onFailure {
                            // https://github.com/zcash/librustzcash/issues/834
                            Log.e("Zcash", "Failed to get memos", it)
                        }
                    }
                }) {
                    val time = kotlinx.datetime.Instant.fromEpochSeconds(it.blockTimeEpochSeconds)
                    val value = if (it.isSentTransaction) {
                        -it.netValue.value
                    } else {
                        it.netValue.value
                    }
                    Text("$time, $value")
                }
            }
        }
    }
}
