package network.bisq.mobile.client.common.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import network.bisq.mobile.client.trusted_node_setup.TrustedNodeSetupScreen
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.graph.addCommonAppRoutes
import network.bisq.mobile.presentation.common.ui.navigation.graph.addScreen
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

@Composable
fun ClientRootNavGraph(
    rootNavController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        modifier = modifier.background(color = BisqTheme.colors.backgroundColor),
        navController = rootNavController,
        startDestination = NavRoute.Splash,
    ) {
        addCommonAppRoutes()
        addClientAppRoutes()
    }
}

fun NavGraphBuilder.addClientAppRoutes() {
    addScreen<TrustedNodeSetupSettings> { TrustedNodeSetupScreen(isWorkflow = false) }
    addScreen<TrustedNodeSetup> { TrustedNodeSetupScreen() }
}
