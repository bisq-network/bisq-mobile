package network.bisq.mobile.presentation.ui.uicases

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import bisqapps.shared.presentation.generated.resources.*
import bisqapps.shared.presentation.generated.resources.Res
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.components.atoms.icons.AddIcon
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.composeModels.BottomNavigationItem
import network.bisq.mobile.presentation.ui.navigation.BottomNavigation
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.navigation.graph.TabNavGraph
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

val navigationListItem = listOf(
    BottomNavigationItem("Home", Routes.TabHome.name, Res.drawable.icon_home),
    BottomNavigationItem("Buy/Sell", Routes.TabCurrencies.name, Res.drawable.icon_market),
    BottomNavigationItem("My Trades", Routes.TabMyTrades.name, Res.drawable.icon_trades),
    BottomNavigationItem("Settings", Routes.TabSettings.name, Res.drawable.icon_settings),
)

interface ITabContainerPresenter : ViewPresenter {
    fun createOffer()
}

@Composable
fun TabContainerScreen() {
    val presenter: ITabContainerPresenter = koinInject()
    val navController: NavHostController = presenter.getRootTabNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute by remember(navBackStackEntry) {
        derivedStateOf {
            navBackStackEntry?.destination?.route
        }
    }

    BisqStaticScaffold(
        topBar = {
            // TODO: Since Topbar should go inside Scaffold
            // the TopBar is written here commonly for all 4 tabs.
            // Based on currentRoute, TopBar customization is done.
            // Ideally, if this goes inside each Tabpage, it will look better.
            // But it's a trade off.
            TopBar(
                isHome = currentRoute == Routes.TabHome.name,
                title = when (currentRoute) {
                    Routes.TabHome.name -> "Home"
                    Routes.TabCurrencies.name -> "Buy/Sell"
                    Routes.TabMyTrades.name -> "My Trades"
                    Routes.TabSettings.name -> "Settings"
                    else -> "App"
                },
                backBehavior = {
                    if (currentRoute != Routes.TabHome.name) {
                        navController.navigate(Routes.TabHome.name) {
                            navController.graph.startDestinationRoute?.let { route ->
                                popUpTo(route) { saveState = false }
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    } else {
                        presenter.showSnackbar("Press back again to exit")
                        presenter.goBack()
                    }
                }
            )

        },
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
        },
        fab = {

            if (currentRoute == Routes.TabCurrencies.name) {
                FloatingActionButton(
                    onClick = { presenter.createOffer() },
                    containerColor = BisqTheme.colors.primary,
                    contentColor = BisqTheme.colors.light1,
                ) {
                    AddIcon(modifier = Modifier.size(24.dp))
                }
            }
        },
        snackbarHostState = presenter.getSnackState(),
        content = { TabNavGraph() }

    )
}
