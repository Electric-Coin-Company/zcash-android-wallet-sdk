package cash.z.ecc.android.sdk.demoapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cash.z.ecc.android.sdk.demoapp.NavigationTargets.HOME
import cash.z.ecc.android.sdk.demoapp.NavigationTargets.SEND
import cash.z.ecc.android.sdk.demoapp.NavigationTargets.WALLET_ADDRESS_DETAILS
import cash.z.ecc.android.sdk.demoapp.ui.screen.addresses.view.Addresses
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.view.Home
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel.WalletViewModel
import cash.z.ecc.android.sdk.demoapp.ui.screen.send.view.Send
import cash.z.ecc.android.sdk.demoapp.util.AndroidApiVersion
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
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
                    resetSdk = { walletViewModel.resetSdk() }
                )
            }
        }
        composable(WALLET_ADDRESS_DETAILS) {
            val synchronizer = walletViewModel.synchronizer.collectAsStateWithLifecycle().value
            if (null == synchronizer) {
                // Display loading indicator
            } else {
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }
                // I don't like giving synchronizer directly over to the view, but for now it isolates each of the
                // demo app views
                Addresses(
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
            if (null == synchronizer || null == walletSnapshot || null == spendingKey) {
                // Display loading indicator
            } else {
                Send(
                    walletSnapshot = walletSnapshot,
                    onSend = {
                        // In the future, consider observing the flow and providing UI updates
                        walletViewModel.send(it)
                        navController.popBackStackJustOnce(SEND)
                    },
                    onBack = { navController.popBackStackJustOnce(SEND) }
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
    val clipboardManager = if (AndroidApiVersion.isAtLeastM) {
        context.getSystemService(ClipboardManager::class.java)
    } else {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    val data = ClipData.newPlainText(
        tag,
        textToCopy
    )
    clipboardManager.setPrimaryClip(data)

    // Notify users with Snackbar only on Android level 32 and lower, as 33 and higher notifies users by its own system
    // way
    if (!AndroidApiVersion.isAtLeastT) {
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
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return intent
}

object NavigationTargets {
    const val HOME = "home" // NON-NLS

    const val WALLET_ADDRESS_DETAILS = "wallet_address_details" // NON-NLS

    const val SEND = "send" // NON-NLS
}
