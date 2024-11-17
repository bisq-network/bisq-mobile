package network.bisq.mobile.presentation.ui.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.navigation.Graph
import network.bisq.mobile.presentation.ui.uicases.GettingStartedScreen
import network.bisq.mobile.presentation.ui.uicases.exchange.ExchangeScreen
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsScreen
import network.bisq.mobile.presentation.ui.uicases.trades.MyTradesScreen

fun NavGraphBuilder.TabNavGraph() {
    navigation(
        startDestination = Routes.TabHome.name,
        route = Graph.MAIN_SCREEN_GRAPH_KEY
    ) {
        composable(route = Routes.TabHome.name) {
            GettingStartedScreen()
        }
        composable(route = Routes.TabExchange.name) {
            ExchangeScreen()
        }
        composable(route = Routes.TabMyTrades.name) {
            MyTradesScreen()
        }
        composable(route = Routes.TabSettings.name) {
            SettingsScreen()
        }
    }

}