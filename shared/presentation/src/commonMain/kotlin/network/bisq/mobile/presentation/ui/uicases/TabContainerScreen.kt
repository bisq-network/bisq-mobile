package network.bisq.mobile.presentation.ui.uicases

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.nav_home
import bisqapps.shared.presentation.generated.resources.nav_more
import bisqapps.shared.presentation.generated.resources.nav_offers
import bisqapps.shared.presentation.generated.resources.nav_trades
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqFABAddButton
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.composeModels.BottomNavigationItem
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.navigation.BottomNavigation
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.navigation.graph.TabNavGraph
import org.koin.compose.koinInject


interface ITabContainerPresenter : ViewPresenter {
    val tradesWithUnreadMessages: StateFlow<Map<String, Int>>
    val showAnimation: StateFlow<Boolean>
    fun createOffer()
}

@Composable
fun TabContainerScreen() {
    val presenter: ITabContainerPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val navController: NavHostController = presenter.getRootTabNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute by remember(navBackStackEntry) {
        derivedStateOf {
            navBackStackEntry?.destination?.route
        }
    }
    val tradesWithUnreadMessages by presenter.tradesWithUnreadMessages.collectAsState()
    val showAnimation by presenter.showAnimation.collectAsState()

    val navigationItems = listOf(
        BottomNavigationItem("mobile.bottomNavigation.home".i18n(), Routes.TabHome.name, Res.drawable.nav_home),
        BottomNavigationItem("mobile.bottomNavigation.offerbook".i18n(), Routes.TabOfferbook.name, Res.drawable.nav_offers),
        BottomNavigationItem("mobile.bottomNavigation.myTrades".i18n(), Routes.TabOpenTradeList.name, Res.drawable.nav_trades),
        BottomNavigationItem("mobile.bottomNavigation.miscItems.tab".i18n(), Routes.TabMiscItems.name, Res.drawable.nav_more),
    )

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
                    Routes.TabHome.name -> ""
                    Routes.TabOfferbook.name -> navigationItems[1].title
                    Routes.TabOpenTradeList.name -> "mobile.bottomNavigation.myOpenTrades".i18n()
                    Routes.TabMiscItems.name -> "mobile.bottomNavigation.miscItems.headline".i18n()
                    else -> "mobile.bottomNavigation.app".i18n()
                },
                backBehavior = {
                    presenter.onMainBackNavigation()
                }
            )

        },
        bottomBar = {
            BottomNavigation(
                items = navigationItems,
                currentRoute = currentRoute.orEmpty(),
                unreadTradeCount = tradesWithUnreadMessages.values.sum(),
                showAnimation = showAnimation,
                onItemClick = { currentNavigationItem ->
                    Routes.fromString(currentNavigationItem.route)?.let { presenter.navigateToTab(it) }
                })
        },
        floatingButton = {
            if (currentRoute == Routes.TabOfferbook.name) {
                BisqFABAddButton(
                    onClick = presenter::createOffer,
                )
            }
        },
        isInteractive = isInteractive,
        snackbarHostState = presenter.getSnackState(),
        content = { TabNavGraph() }

    )
}
