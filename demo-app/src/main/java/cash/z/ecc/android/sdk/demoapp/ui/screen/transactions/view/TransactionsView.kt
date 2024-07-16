package cash.z.ecc.android.sdk.demoapp.ui.screen.transactions.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.demoapp.util.toTransactionState
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.model.WalletAddresses
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

@Composable
@Suppress("ktlint:standard:function-naming", "standard:function-naming")
fun Transactions(
    synchronizer: Synchronizer,
    onBack: () -> Unit
) {
    val queryScope = rememberCoroutineScope()

    // TODO [#846]: Slow addresses providing
    // TODO [#846]: https://github.com/zcash/zcash-android-wallet-sdk/issues/846

    val stateFlow by remember(synchronizer) {
        mutableStateOf(
            flow<WalletAddresses?> { emit(WalletAddresses.new(synchronizer)) }.catch { emit(null) }
        )
    }
    val walletAddresses by stateFlow.collectAsStateWithLifecycle(initialValue = null)

    val transactions =
        if (walletAddresses == null) {
            emptyList()
        } else {
            synchronizer
                .transactions
                .collectAsStateWithLifecycle(initialValue = emptyList())
                .value
                .map { transactionOverview ->
                    transactionOverview.toTransactionState(
                        context = LocalContext.current,
                        onClick = {
                            queryScope.launch {
                                synchronizer.getMemos(transactionOverview).toList().run {
                                    Twig.info {
                                        "Transaction memos: count: $size, contains: ${joinToString().ifEmpty { "-" }}"
                                    }
                                }
                            }
                        }
                    )
                }
        }.toImmutableList()

    TransactionsInternal(
        onBack = onBack,
        onRefresh = { (synchronizer as SdkSynchronizer).refreshTransactions() },
        transactions = transactions
    )
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun TransactionsInternal(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    transactions: ImmutableList<TransactionState>
) {
    Scaffold(
        topBar = {
            TransactionsTopAppBar(
                onBack = onBack,
                onRefresh = onRefresh,
            )
        }
    ) { paddingValues ->
        Column(
            Modifier.padding(top = paddingValues.calculateTopPadding()),
        ) {
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                itemsIndexed(transactions) { index, state ->
                    Transaction(state)
                    if (index != transactions.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ktlint:standard:function-naming")
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

@Preview
@Composable
@Suppress("ktlint:standard:function-naming")
private fun TransactionsPreview() =
    MaterialTheme {
        TransactionsInternal(
            onBack = {},
            onRefresh = {},
            transactions =
                listOf(
                    TransactionState(
                        time = "time",
                        value = "value",
                        fee = "fee",
                        status = "status",
                        onClick = {},
                    ),
                    TransactionState(
                        time = "time",
                        value = "value",
                        fee = "fee",
                        status = "status",
                        onClick = {},
                    ),
                ).toImmutableList()
        )
    }
