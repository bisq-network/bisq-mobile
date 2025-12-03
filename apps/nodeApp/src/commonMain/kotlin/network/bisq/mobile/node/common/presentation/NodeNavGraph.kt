package network.bisq.mobile.node.common.presentation

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.graph.addCommonAppRoutes
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

@Composable
fun NodeNavGraph(rootNavController: NavHostController) {
    NavHost(
        modifier = Modifier.background(color = BisqTheme.colors.backgroundColor),
        navController = rootNavController,
        startDestination = NavRoute.Splash,
    ) {
        addCommonAppRoutes()
    }
}