package network.bisq.mobile.client.common.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import network.bisq.mobile.presentation.ui.navigation.NavRoute
import network.bisq.mobile.presentation.ui.navigation.graph.addScreen
import network.bisq.mobile.presentation.ui.navigation.graph.addCommonAppRoutes
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.startup.TrustedNodeSetupScreen

@Composable
fun ClientRootNavGraph(rootNavController: NavHostController) {
    NavHost(
        modifier = Modifier.background(color = BisqTheme.colors.backgroundColor),
        navController = rootNavController,
        startDestination = NavRoute.Splash,
    ) {
        addCommonAppRoutes()
        addClientAppRoutes()
    }
}

fun NavGraphBuilder.addClientAppRoutes() {
    addScreen<NavRoute.TrustedNodeSetupSettings> { TrustedNodeSetupScreen(false) }
    addScreen<NavRoute.TrustedNodeSetup> { TrustedNodeSetupScreen() }
}