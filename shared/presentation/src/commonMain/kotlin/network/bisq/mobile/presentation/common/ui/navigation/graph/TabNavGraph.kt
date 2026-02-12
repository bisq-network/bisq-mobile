package network.bisq.mobile.presentation.common.ui.navigation.graph

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.NavUtils
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.tabs.dashboard.DashboardScreen
import network.bisq.mobile.presentation.tabs.more.MiscItemsScreen
import network.bisq.mobile.presentation.tabs.offers.OfferbookMarketScreen
import network.bisq.mobile.presentation.tabs.open_trades.OpenTradeListScreen
import org.koin.compose.koinInject

@ExcludeFromCoverage
@Composable
fun TabNavGraph(navController: NavHostController) {
    val navigationManager: NavigationManager = koinInject()

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
                deepLinks =
                    listOf(
                        navDeepLink<NavRoute.TabOpenTradeList>(
                            basePath = NavUtils.getDeepLinkBasePath<NavRoute.TabOpenTradeList>(),
                        ),
                    ),
            ) {
                OpenTradeListScreen()
            }

            composable<NavRoute.TabMiscItems> {
                MiscItemsScreen()
            }
        }
    }

    DisposableEffect(navController) {
        navigationManager.setTabNavController(navController)
        onDispose {
            navigationManager.setTabNavController(null)
        }
    }
}
