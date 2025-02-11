package network.bisq.mobile.presentation.ui.navigation.graph

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.navigation.Graph
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.GettingStartedScreen
import network.bisq.mobile.presentation.ui.uicases.offerbook.OfferbookMarketScreen
import network.bisq.mobile.presentation.ui.uicases.open_trades.OpenTradeListScreen
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsScreen
import org.koin.compose.koinInject

@Composable
fun TabNavGraph() {

    val mainPresenter: AppPresenter = koinInject()
    val selectedTab = remember { mutableStateOf(Routes.TabHome.name) }
    val navController = mainPresenter.getRootTabNavController()
    val viewModelStoreOwner = LocalViewModelStoreOwner.current
    DisposableEffect(viewModelStoreOwner) {
        navController.setViewModelStore(viewModelStoreOwner!!.viewModelStore)
        onDispose {}
    }

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
                GettingStartedScreen()
            }
            composable(route = Routes.TabOfferbook.name) {
                selectedTab.value = Routes.TabOfferbook.name
                OfferbookMarketScreen()
            }
            composable(route = Routes.TabOpenTradeList.name) {
                selectedTab.value = Routes.TabOpenTradeList.name
                OpenTradeListScreen()
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