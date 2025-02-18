package network.bisq.mobile.presentation.ui.uicases

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_home
import bisqapps.shared.presentation.generated.resources.icon_market
import bisqapps.shared.presentation.generated.resources.icon_settings
import bisqapps.shared.presentation.generated.resources.icon_trades
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqFABAddButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.AddIcon
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.composeModels.BottomNavigationItem
import network.bisq.mobile.presentation.ui.navigation.BottomNavigation
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.navigation.graph.TabNavGraph
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

val navigationListItem = listOf(
    BottomNavigationItem("Home", Routes.TabHome.name, Res.drawable.icon_home),
    BottomNavigationItem("Offerbook", Routes.TabOfferbook.name, Res.drawable.icon_market),
    BottomNavigationItem("My Trades", Routes.TabOpenTradeList.name, Res.drawable.icon_trades),
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
                    Routes.TabOfferbook.name -> "Offerbook"
                    Routes.TabOpenTradeList.name -> "My Open Trades"
                    Routes.TabSettings.name -> "Settings"
                    else -> "App"
                },
                backBehavior = {
                    presenter.onMainBackNavigation()
                }
            )

        },
        bottomBar = {
            BottomNavigation(
                items = navigationListItem,
                currentRoute = currentRoute.orEmpty(),
                onItemClick = { currentNavigationItem ->
                    Routes.fromString(currentNavigationItem.route)?.let { presenter.navigateToTab(it) }
                })
        },
        fab = {
            if (currentRoute == Routes.TabOfferbook.name) {
                BisqFABAddButton(
                    onClick = { presenter.createOffer() },
                )
            }
        },
        snackbarHostState = presenter.getSnackState(),
        content = { TabNavGraph() }

    )
}
