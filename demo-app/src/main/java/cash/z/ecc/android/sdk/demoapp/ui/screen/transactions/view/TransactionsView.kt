package cash.z.ecc.android.sdk.demoapp.ui.screen.transactions.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.WalletAddresses
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

@Preview(name = "Transactions")
@Composable
private fun ComposablePreview() {
    MaterialTheme {
        // TODO [#1090]: Demo: Add Addresses and Transactions Compose Previews
        // TODO [#1090]: https://github.com/zcash/zcash-android-wallet-sdk/issues/1090
        // TransactionsView()
    }
}

@Composable
fun Transactions(
    synchronizer: Synchronizer,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TransactionsTopAppBar(
                onBack,
                onRefresh = {
                    (synchronizer as SdkSynchronizer).refreshTransactions()
                }
            )
        }
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
                    .toPersistentList()
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TransactionsTopAppBar(
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
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
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Outlined.Autorenew,
                    contentDescription = stringResource(id = R.string.transactions_refresh)
                )
            }
        }
    )
}

@Composable
private fun TransactionsMainContent(
    paddingValues: PaddingValues,
    synchronizer: Synchronizer,
    transactions: ImmutableList<TransactionOverview>
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
                        memos.toList().run {
                            Twig.info {
                                "Transaction memos: count: $size, contains: ${joinToString().ifEmpty { "-" }}"
                            }
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
