package cash.z.ecc.android.sdk.demoapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
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
                    goSend = { navController.navigateJustOnce(SEND) },
                    goAddressDetails = { navController.navigateJustOnce(WALLET_ADDRESS_DETAILS) },
                    resetSdk = { walletViewModel.resetSdk() }
                )
            }
        }
        composable(WALLET_ADDRESS_DETAILS) {
            val synchronizer = walletViewModel.synchronizer.collectAsStateWithLifecycle().value
            if (null == synchronizer) {
                // Display loading indicator
            } else {
                // I don't like giving synchronizer directly over to the view, but for now it isolates each of the
                // demo app views
                Addresses(
                    synchronizer = synchronizer,
                    copyToClipboard = { tag, textToCopy ->
                        copyToClipboard(applicationContext, tag, textToCopy)
                    },
                    onBack = { navController.popBackStackJustOnce(WALLET_ADDRESS_DETAILS) }
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

fun copyToClipboard(context: Context, tag: String, textToCopy: String) {
    val clipboardManager = context.getSystemService(ClipboardManager::class.java)
    val data = ClipData.newPlainText(
        tag,
        textToCopy
    )
    clipboardManager.setPrimaryClip(data)
}

object NavigationTargets {
    const val HOME = "home"

    const val WALLET_ADDRESS_DETAILS = "wallet_address_details"

    const val SEND = "send"
}