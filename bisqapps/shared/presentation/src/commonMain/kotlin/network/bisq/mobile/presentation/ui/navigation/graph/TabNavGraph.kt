package network.bisq.mobile.presentation.ui.navigation.graph

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.compose.runtime.Composable
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.navigation.Graph
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.GettingStartedScreen
import network.bisq.mobile.presentation.ui.uicases.exchange.ExchangeScreen
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsScreen
import network.bisq.mobile.presentation.ui.uicases.trades.MyTradesScreen
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun TabNavGraph() {

    val navController: NavHostController = koinInject(named("TabNavController"))

    NavHost(
        modifier = Modifier.background(color = BisqTheme.colors.backgroundColor),
        navController = navController,
        startDestination = Graph.MAIN_SCREEN_GRAPH_KEY,
    ) {
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

}