package network.bisq.mobile.presentation.ui.navigation.graph

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.navigation.Graph
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.GettingStartedScreen
import network.bisq.mobile.presentation.ui.uicases.offer.MarketListScreen
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsScreen
import network.bisq.mobile.presentation.ui.uicases.trades.MyTradesScreen
import org.koin.compose.koinInject

@Composable
fun TabNavGraph() {

    val mainPresenter: AppPresenter = koinInject()
    val selectedTab = remember { mutableStateOf(Routes.TabHome.name) }

    NavHost(
        modifier = Modifier.background(color = BisqTheme.colors.backgroundColor),
        navController = mainPresenter.getRootTabNavController(),
        startDestination = Graph.MAIN_SCREEN_GRAPH_KEY,
    ) {
        navigation(
            startDestination = Routes.TabHome.name,
            route = Graph.MAIN_SCREEN_GRAPH_KEY
        ) {
            composable(route = Routes.TabHome.name) {
                selectedTab.value = Routes.TabHome.name
                GettingStartedScreen()
            }
            composable(route = Routes.TabCurrencies.name) {
                selectedTab.value = Routes.TabCurrencies.name
                MarketListScreen()
            }
            composable(route = Routes.TabMyTrades.name) {
                selectedTab.value = Routes.TabMyTrades.name
                MyTradesScreen()
            }
            composable(route = Routes.TabSettings.name) {
                selectedTab.value = Routes.TabSettings.name
                SettingsScreen(
                    isTabSelected = selectedTab.value == Routes.TabSettings.name
                )
            }
        }
    }

}