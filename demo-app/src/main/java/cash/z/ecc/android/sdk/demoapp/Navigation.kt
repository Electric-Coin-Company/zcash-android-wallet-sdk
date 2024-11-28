package cash.z.ecc.android.sdk.demoapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.demoapp.NavigationTargets.BALANCE
import cash.z.ecc.android.sdk.demoapp.NavigationTargets.HOME
import cash.z.ecc.android.sdk.demoapp.NavigationTargets.SEND
import cash.z.ecc.android.sdk.demoapp.NavigationTargets.SERVER
import cash.z.ecc.android.sdk.demoapp.NavigationTargets.TRANSACTIONS
import cash.z.ecc.android.sdk.demoapp.NavigationTargets.WALLET_ADDRESS_DETAILS
import cash.z.ecc.android.sdk.demoapp.ext.Testnet
import cash.z.ecc.android.sdk.demoapp.ui.screen.addresses.view.Addresses
import cash.z.ecc.android.sdk.demoapp.ui.screen.balance.view.Balance
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.view.Home
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel.SecretState
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel.WalletViewModel
import cash.z.ecc.android.sdk.demoapp.ui.screen.send.view.Send
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.Server
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.YW_HOST_1
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.YW_HOST_2
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.YW_HOST_3
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.YW_HOST_4
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.YW_HOST_5
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.YW_HOST_6
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.YW_HOST_7
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.YW_HOST_8
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.YW_PORT
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.ZR_HOST
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.ZR_HOST_AP
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.ZR_HOST_EU
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.ZR_HOST_NA
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.ZR_HOST_SA
import cash.z.ecc.android.sdk.demoapp.ui.screen.server.view.ZR_PORT
import cash.z.ecc.android.sdk.demoapp.ui.screen.transactions.view.Transactions
import cash.z.ecc.android.sdk.demoapp.util.AndroidApiVersion
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.model.FastestServersResult
import cash.z.ecc.android.sdk.model.Proposal
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.type.ServerValidation
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod", "ktlint:standard:function-naming", "standard:function-naming")
internal fun ComposeActivity.Navigation() {
    val navController = rememberNavController()

    val walletViewModel by viewModels<WalletViewModel>()

    NavHost(navController = navController, startDestination = HOME) {
        composable(HOME) {
            val walletSnapshot = walletViewModel.walletSnapshot.collectAsStateWithLifecycle().value
            if (null == walletSnapshot) {
                // Display loading indicator
            } else {
                Home(
                    walletSnapshot,
                    goBalance = { navController.navigateJustOnce(BALANCE) },
                    goSend = { navController.navigateJustOnce(SEND) },
                    goAddressDetails = { navController.navigateJustOnce(WALLET_ADDRESS_DETAILS) },
                    isTestnet = ZcashNetwork.fromResources(applicationContext) == ZcashNetwork.Testnet,
                    goTestnetFaucet = {
                        runCatching {
                            startActivity(newBrowserIntent("https://faucet.zecpages.com/")) // NON-NLS
                        }.onFailure {
                            // This could fail on devices without a browser.
                            // An improvement here in the future would be showing a snackbar or error dialog.
                        }
                    },
                    goTransactions = { navController.navigateJustOnce(TRANSACTIONS) },
                    goServer = { navController.navigateJustOnce(SERVER) },
                    resetSdk = { walletViewModel.resetSdk() },
                    rewind = { walletViewModel.rewind() }
                )
            }
        }
        composable(BALANCE) {
            val synchronizer = walletViewModel.synchronizer.collectAsStateWithLifecycle().value
            val walletSnapshot = walletViewModel.walletSnapshot.collectAsStateWithLifecycle().value
            if (null == synchronizer || null == walletSnapshot) {
                // Display loading indicator
            } else {
                val balance = walletSnapshot.balanceByAccount(walletViewModel.getCurrentAccount())
                val scope = rememberCoroutineScope()
                Balance(
                    exchangeRateUsd = walletSnapshot.exchangeRateUsd,
                    accountBalance = balance,
                    onShieldFunds = { walletViewModel.shieldFunds() },
                    sendState = walletViewModel.sendState.collectAsStateWithLifecycle().value,
                    onBack = {
                        walletViewModel.clearSendOrShieldState()
                        navController.popBackStackJustOnce(BALANCE)
                    },
                    onRefresh = {
                        scope.launch {
                            (synchronizer as SdkSynchronizer).refreshAllBalances()
                            synchronizer.refreshExchangeRateUsd()
                        }
                    }
                )
            }
        }
        composable(WALLET_ADDRESS_DETAILS) {
            val synchronizer = walletViewModel.synchronizer.collectAsStateWithLifecycle().value
            if (null == synchronizer) {
                // Display loading indicator
            } else {
                val currentAccount = walletViewModel.getCurrentAccount()
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }
                // I don't like giving synchronizer directly over to the view, but for now it isolates each of the
                // demo app views
                Addresses(
                    account = currentAccount,
                    synchronizer = synchronizer,
                    copyToClipboard = { tag, textToCopy ->
                        copyToClipboard(
                            applicationContext,
                            tag,
                            textToCopy,
                            scope,
                            snackbarHostState
                        )
                    },
                    onBack = { navController.popBackStackJustOnce(WALLET_ADDRESS_DETAILS) },
                    snackbarHostState = snackbarHostState
                )
            }
        }
        composable(SEND) {
            val synchronizer = walletViewModel.synchronizer.collectAsStateWithLifecycle().value
            val walletSnapshot = walletViewModel.walletSnapshot.collectAsStateWithLifecycle().value
            val spendingKey = walletViewModel.spendingKey.collectAsStateWithLifecycle().value

            val sendTransactionProposal = remember { mutableStateOf<Proposal?>(null) }

            val sendTransactionProposalFromUri = remember { mutableStateOf<Proposal?>(null) }

            if (null == synchronizer || null == walletSnapshot || null == spendingKey) {
                // Display loading indicator
            } else {
                val currentAccount = walletViewModel.getCurrentAccount()
                Send(
                    accountBalance = walletSnapshot.balanceByAccount(currentAccount),
                    sendState = walletViewModel.sendState.collectAsStateWithLifecycle().value,
                    onSend = {
                        walletViewModel.send(it)
                    },
                    onGetProposal = {
                        sendTransactionProposal.value = walletViewModel.getSendProposal(it)
                    },
                    onGetProposalFromUri = {
                        sendTransactionProposalFromUri.value = walletViewModel.getSendProposalFromUri(it)
                    },
                    onBack = {
                        walletViewModel.clearSendOrShieldState()
                        navController.popBackStackJustOnce(SEND)
                    },
                    sendTransactionProposal = sendTransactionProposal.value,
                    sendTransactionProposalFromUri = sendTransactionProposalFromUri.value
                )
            }
        }
        composable(SERVER) {
            val secretState = walletViewModel.secretState.collectAsStateWithLifecycle().value

            val synchronizer = walletViewModel.synchronizer.collectAsStateWithLifecycle().value

            val context = LocalContext.current

            Twig.info { "Current secrets state: $secretState" }

            if (synchronizer == null || secretState !is SecretState.Ready) {
                // Display loading indicator
            } else {
                val wallet = secretState.persistableWallet

                Twig.info { "Current persisted wallet: ${wallet.toSafeString()}" }

                val scope = rememberCoroutineScope()

                var validationResult: ServerValidation by remember { mutableStateOf(ServerValidation.Valid) }

                val onBack = {
                    if (validationResult !is ServerValidation.Running) {
                        navController.popBackStackJustOnce(SERVER)
                    }
                }

                BackHandler { onBack() }

                val fastestServers = remember { mutableStateOf<FastestServersResult?>(null) }

                LaunchedEffect(Unit) {
                    synchronizer.getFastestServers(
                        context = context,
                        servers =
                            buildList {
                                if (ZcashNetwork.fromResources(application) == ZcashNetwork.Mainnet) {
                                    add(LightWalletEndpoint(ZR_HOST, ZR_PORT, true))
                                    add(LightWalletEndpoint(ZR_HOST_NA, ZR_PORT, true))
                                    add(LightWalletEndpoint(ZR_HOST_SA, ZR_PORT, true))
                                    add(LightWalletEndpoint(ZR_HOST_EU, ZR_PORT, true))
                                    add(LightWalletEndpoint(ZR_HOST_AP, ZR_PORT, true))
                                    add(LightWalletEndpoint(YW_HOST_1, YW_PORT, true))
                                    add(LightWalletEndpoint(YW_HOST_2, YW_PORT, true))
                                    add(LightWalletEndpoint(YW_HOST_3, YW_PORT, true))
                                    add(LightWalletEndpoint(YW_HOST_4, YW_PORT, true))
                                    add(LightWalletEndpoint(YW_HOST_5, YW_PORT, true))
                                    add(LightWalletEndpoint(YW_HOST_6, YW_PORT, true))
                                    add(LightWalletEndpoint(YW_HOST_7, YW_PORT, true))
                                    add(LightWalletEndpoint(YW_HOST_8, YW_PORT, true))
                                } else {
                                    add(LightWalletEndpoint.Testnet)
                                }
                            }
                    ).collect {
                        fastestServers.value = it
                    }
                }

                Server(
                    buildInNetwork = ZcashNetwork.fromResources(application),
                    onBack = onBack,
                    onServerChange = { newEndpoint ->
                        scope.launch {
                            validationResult = ServerValidation.Running
                            validationResult = synchronizer.validateServerEndpoint(application, newEndpoint)

                            Twig.info { "Validation result: $validationResult" }

                            when (validationResult) {
                                ServerValidation.Valid -> {
                                    walletViewModel.closeSynchronizer()

                                    val newWallet =
                                        wallet.copy(
                                            endpoint = newEndpoint
                                        )

                                    Twig.info { "New wallet: ${newWallet.toSafeString()}" }

                                    walletViewModel.persistExistingWallet(newWallet)

                                    Toast.makeText(
                                        applicationContext,
                                        "Server saved",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                is ServerValidation.InValid -> {
                                    Twig.error { "Failed to validate the new endpoint: $newEndpoint" }
                                }

                                else -> {
                                    // Should not happen
                                    Twig.info { "Server validation state: $validationResult" }
                                }
                            }
                        }
                    },
                    validationResult = validationResult,
                    wallet = wallet,
                    fastestServers = fastestServers.value
                )
            }
        }
        composable(TRANSACTIONS) {
            val synchronizer = walletViewModel.synchronizer.collectAsStateWithLifecycle().value
            if (null == synchronizer) {
                // Display loading indicator
            } else {
                val currentAccount = walletViewModel.getCurrentAccount()
                Transactions(
                    account = currentAccount,
                    synchronizer = synchronizer,
                    onBack = { navController.popBackStackJustOnce(TRANSACTIONS) }
                )
            }
        }
    }
}

private fun NavHostController.navigateJustOnce(
    route: String,
    navOptionsBuilder: (NavOptionsBuilder.() -> Unit)? = null
) {
    if (currentDestination?.route == route) {
        return
    }

    if (navOptionsBuilder != null) {
        navigate(route, navOptionsBuilder)
    } else {
        navigate(route)
    }
}

/**
 * Pops up the current screen from the back stack. Parameter currentRouteToBePopped is meant to be
 * set only to the current screen so we can easily debounce multiple screen popping from the back stack.
 *
 * @param currentRouteToBePopped current screen which should be popped up.
 */
private fun NavHostController.popBackStackJustOnce(currentRouteToBePopped: String) {
    if (currentDestination?.route != currentRouteToBePopped) {
        return
    }
    popBackStack()
}

private fun copyToClipboard(
    context: Context,
    tag: String,
    textToCopy: String,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    val clipboardManager = context.getSystemService(ClipboardManager::class.java)

    val data =
        ClipData.newPlainText(
            tag,
            textToCopy
        )
    clipboardManager.setPrimaryClip(data)

    // Notify users with Snackbar only on Android level 32 and lower, as 33 and higher notifies users by its own system
    // way
    if (!AndroidApiVersion.isAtLeastTiramisu) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.address_copied, textToCopy),
                duration = SnackbarDuration.Short
            )
        }
    }
}

private fun newBrowserIntent(url: String): Intent {
    val uri = Uri.parse(url)
    val intent =
        Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    return intent
}

object NavigationTargets {
    const val HOME = "home" // NON-NLS

    const val BALANCE = "balance" // NON-NLS

    const val WALLET_ADDRESS_DETAILS = "wallet_address_details" // NON-NLS

    const val SEND = "send" // NON-NLS

    const val SERVER = "server" // NON-NLS

    const val TRANSACTIONS = "transactions" // NON-NLS
}
