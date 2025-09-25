package network.bisq.mobile.presentation.ui.navigation.graph

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import network.bisq.mobile.presentation.ui.navigation.Graph
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.DashboardScreen
import network.bisq.mobile.presentation.ui.uicases.offerbook.OfferbookMarketScreen
import network.bisq.mobile.presentation.ui.uicases.open_trades.OpenTradeListScreen
import network.bisq.mobile.presentation.ui.uicases.settings.MiscItemsScreen

@Composable
fun TabNavGraph(navController: NavHostController) {

    val selectedTab = remember { mutableStateOf(Routes.TabHome.name) }

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
                selectedTab.value = Routes.TabHome.name
                DashboardScreen()
            }
            composable(route = Routes.TabOfferbook.name) {
                selectedTab.value = Routes.TabOfferbook.name
                OfferbookMarketScreen()
            }
            composable(route = Routes.TabOpenTradeList.name) {
                selectedTab.value = Routes.TabOpenTradeList.name
                OpenTradeListScreen()
            }
            composable(route = Routes.TabMiscItems.name) {
                selectedTab.value = Routes.TabMiscItems.name
                MiscItemsScreen()
            }
        }
    }

}