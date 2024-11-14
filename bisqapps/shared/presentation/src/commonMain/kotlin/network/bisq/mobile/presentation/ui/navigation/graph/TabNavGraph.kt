package network.bisq.mobile.presentation.ui.navigation.graph

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.navigation.Graph
import network.bisq.mobile.presentation.ui.uicases.GettingStartedScreen
import network.bisq.mobile.presentation.ui.uicases.exchange.ExchangeScreen
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsScreen
import network.bisq.mobile.presentation.ui.uicases.trades.MyTradesScreen

fun NavGraphBuilder.TabNavGraph(
    rootNavController: NavHostController,
    innerPadding: PaddingValues
) {
    navigation(
        startDestination = Routes.TabHome.name,
        route = Graph.MainScreenGraph
    ) {
        composable(route = Routes.TabHome.name) {
            GettingStartedScreen(rootNavController = rootNavController, innerPadding = innerPadding)
        }
        composable(route = Routes.TabExchange.name) {
            ExchangeScreen(rootNavController = rootNavController, innerPadding = innerPadding)
        }
        composable(route = Routes.TabMyTrades.name) {
            MyTradesScreen(rootNavController = rootNavController, innerPadding = innerPadding)
        }
        composable(route = Routes.TabSettings.name) {
            SettingsScreen(rootNavController = rootNavController, innerPadding = innerPadding)
        }
    }

}