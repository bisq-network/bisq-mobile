package network.bisq.mobile.client.common.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.toRoute
import network.bisq.mobile.client.common.presentation.support.ClientSupportScreen
import network.bisq.mobile.client.trusted_node_setup.TrustedNodeSetupScreen
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.graph.addCommonAppRoutes
import network.bisq.mobile.presentation.common.ui.navigation.graph.addScreen
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
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

    NavHost(
        modifier = modifier.background(color = BisqTheme.colors.backgroundColor),
        navController = rootNavController,
        startDestination = NavRoute.Splash,
    ) {
        addCommonAppRoutes()
        addClientAppRoutes()
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
fun NavGraphBuilder.addClientAppRoutes() {
    // Override Support screen with client-specific version
    addScreen<NavRoute.Support> { ClientSupportScreen() }

    // Client-specific screens
    addScreen<TrustedNodeSetupSettings> { TrustedNodeSetupScreen(isWorkflow = false) }
    addScreen<TrustedNodeSetup> { entry ->
        val showConnectionFailed = entry.toRoute<TrustedNodeSetup>().showConnectionFailed
        TrustedNodeSetupScreen(showConnectionFailed = showConnectionFailed)
    }
}
