package network.bisq.mobile.presentation.ui.navigation.graph

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import network.bisq.mobile.presentation.ui.navigation.NavRoute
import network.bisq.mobile.presentation.ui.navigation.NavUtils
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.DashboardScreen
import network.bisq.mobile.presentation.ui.uicases.offerbook.OfferbookMarketScreen
import network.bisq.mobile.presentation.ui.uicases.open_trades.OpenTradeListScreen
import network.bisq.mobile.presentation.ui.uicases.settings.MiscItemsScreen

@Composable
fun TabNavGraph(navController: NavHostController) {
    NavHost(
        modifier = Modifier.background(color = BisqTheme.colors.backgroundColor),
        navController = navController,
        startDestination = NavRoute.HomeScreenGraphKey,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        navigation<NavRoute.HomeScreenGraphKey>(
            startDestination = NavRoute.TabHome,
        ) {
            composable<NavRoute.TabHome> {
                DashboardScreen()
            }

            composable<NavRoute.TabOfferbookMarket> {
                OfferbookMarketScreen()
            }

            composable<NavRoute.TabOpenTradeList>(
                deepLinks = listOf(
                    navDeepLink<NavRoute.TabOpenTradeList>(
                        basePath = NavUtils.getDeepLinkBasePath<NavRoute.TabOpenTradeList>()
                    )
                ),
            ) {
                OpenTradeListScreen()
            }

            composable<NavRoute.TabMiscItems> {
                MiscItemsScreen()
            }
        }
    }
}
