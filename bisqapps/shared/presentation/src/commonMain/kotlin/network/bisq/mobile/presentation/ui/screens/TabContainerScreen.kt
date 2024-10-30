package network.bisq.mobile.presentation.ui.screens

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import network.bisq.mobile.presentation.ui.model.BottomNavigationItem
import network.bisq.mobile.presentation.ui.navigation.BottomNavigation
import network.bisq.mobile.presentation.ui.navigation.Graph
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.navigation.graph.RootNavGraph
import network.bisq.mobile.presentation.ui.theme.secondaryColor

val navigationListItem = listOf(
    BottomNavigationItem("Home", Routes.TabHome.name, "drawable/home.svg"),
    BottomNavigationItem("Buy/Sell", Routes.TabExchange.name, "drawable/market.svg"),
    BottomNavigationItem("My Trades", Routes.TabMyTrades.name, "drawable/trades.svg"),
    BottomNavigationItem("Settings", Routes.TabSettings.name, "drawable/settings.svg"),
)


@Composable
fun TabContainerScreen( rootNavController: NavController) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute by remember(navBackStackEntry) {
        derivedStateOf {
            navBackStackEntry?.destination?.route
        }
    }

    Scaffold(
        containerColor = secondaryColor,
        bottomBar = {
            BottomNavigation(
                items = navigationListItem,
                currentRoute = currentRoute.orEmpty(),
                onItemClick = { currentNavigationItem ->
                    navController.navigate(currentNavigationItem.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
        }

    ) { innerPadding ->
        RootNavGraph(rootNavController = navController,innerPadding = innerPadding, startDestination = Graph.MainScreenGraph)
    }
}
