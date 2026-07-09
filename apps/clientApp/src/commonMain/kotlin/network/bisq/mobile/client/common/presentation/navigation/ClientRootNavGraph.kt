package network.bisq.mobile.client.common.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import network.bisq.mobile.client.common.presentation.support.ClientSupportScreen
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.CreatePaymentAccountScreen
import network.bisq.mobile.client.payment_accounts.presentation.payment_account_detail.PaymentAccountMusigDetailScreen
import network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list.PaymentAccountsMusigScreen
import network.bisq.mobile.client.splash.ClientSplashScreen
import network.bisq.mobile.client.trusted_node_setup.TrustedNodeSetupScreen
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.animation.AnimationSettings
import network.bisq.mobile.presentation.common.ui.components.ErrorState
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.graph.addCommonAppRoutes
import network.bisq.mobile.presentation.common.ui.navigation.graph.addScreen
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.koin.compose.koinInject

// TODO: Coverage exclusion rationale - Compose UI navigation code cannot be unit tested.
// Requires Compose UI testing framework for proper coverage.
@ExcludeFromCoverage
@Composable
fun ClientRootNavGraph(
    rootNavController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val navigationManager: NavigationManager = koinInject()
    val animationSettings: AnimationSettings = koinInject()
    val animationsEnabled: () -> Boolean = { animationSettings.enabled.value }

    NavHost(
        modifier = modifier.background(color = BisqTheme.colors.backgroundColor),
        navController = rootNavController,
        startDestination = NavRoute.Splash(),
    ) {
        composable<NavRoute.Splash> { backStackEntry ->
            val route: NavRoute.Splash = backStackEntry.toRoute()
            ClientSplashScreen(route)
        }
        addCommonAppRoutes(animationsEnabled)
        addClientAppRoutes(animationsEnabled)
    }

    DisposableEffect(rootNavController) {
        navigationManager.setRootNavController(rootNavController)
        onDispose {
            navigationManager.setRootNavController(null)
        }
    }
}

// TODO: Coverage exclusion rationale - NavGraphBuilder extension for Compose navigation.
@ExcludeFromCoverage
fun NavGraphBuilder.addClientAppRoutes(animationsEnabled: () -> Boolean) {
    // Override Support screen with client-specific version
    addScreen<NavRoute.Support>(animationsEnabled = animationsEnabled) { ClientSupportScreen() }

    // Client-specific screens
    addScreen<ClientNavRoute.TrustedNodeSetupSettings>(animationsEnabled = animationsEnabled) { TrustedNodeSetupScreen(isWorkflow = false) }
    addScreen<ClientNavRoute.TrustedNodeSetup>(animationsEnabled = animationsEnabled) { entry ->
        val route = entry.toRoute<ClientNavRoute.TrustedNodeSetup>()
        TrustedNodeSetupScreen(
            showConnectionFailed = route.showConnectionFailed,
            showKeystoreError = route.showKeystoreError,
            showSubscriptionsFailed = route.showSubscriptionsFailed,
        )
    }

    addScreen<ClientNavRoute.PaymentAccountsMusig>(animationsEnabled = animationsEnabled) { PaymentAccountsMusigScreen() }
    addScreen<ClientNavRoute.PaymentAccountsMusigDetail>(animationsEnabled = animationsEnabled) { backStackEntry ->
        val route: ClientNavRoute.PaymentAccountsMusigDetail = backStackEntry.toRoute()
        PaymentAccountMusigDetailScreen(accountName = route.accountName)
    }
    addScreen<ClientNavRoute.CreatePaymentAccount>(animationsEnabled = animationsEnabled) { backStackEntry ->
        val route: ClientNavRoute.CreatePaymentAccount = backStackEntry.toRoute()
        val accountType = runCatching { PaymentAccountType.valueOf(route.accountTypeName) }.getOrNull()

        if (accountType == null) {
            ErrorState(message = "mobile.error.generic".i18n())
        } else {
            CreatePaymentAccountScreen(accountType = accountType)
        }
    }
}
