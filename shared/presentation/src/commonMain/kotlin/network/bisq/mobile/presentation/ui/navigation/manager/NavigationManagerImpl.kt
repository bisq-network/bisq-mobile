package network.bisq.mobile.presentation.ui.navigation.manager

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavUri
import androidx.navigation.navOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.ui.navigation.NavRoute
import network.bisq.mobile.presentation.ui.navigation.TabNavRoute

private const val GET_TIMEOUT = 5000L

class NavigationManagerImpl(
    val coroutineJobsManager: CoroutineJobsManager,
) : NavigationManager, Logging {

    private var rootNavControllerFlow = MutableStateFlow<NavHostController?>(null)
    private var tabNavControllerFlow = MutableStateFlow<NavHostController?>(null)

    private val _currentTab = MutableStateFlow<TabNavRoute?>(null)
    override val currentTab: StateFlow<TabNavRoute?> = _currentTab.asStateFlow()
    private var tabDestinationListener: NavController.OnDestinationChangedListener? = null

    private val scope get() = coroutineJobsManager.getScope()

    private suspend fun getRootNavController(): NavHostController? {
        val controller = withTimeoutOrNull(GET_TIMEOUT) {
            rootNavControllerFlow.mapNotNull { it }.first()
        }
        if (controller == null) {
            log.e { "Timed out waiting for root nav controller after ${GET_TIMEOUT}ms" }
        }
        return controller
    }

    private suspend fun getTabNavController(): NavHostController? {
        val controller = withTimeoutOrNull(GET_TIMEOUT) {
            tabNavControllerFlow.mapNotNull { it }.first()
        }
        if (controller == null) {
            log.e { "Timed out waiting for tab nav controller after ${GET_TIMEOUT}ms" }
        }
        return controller
    }

    override fun setRootNavController(navController: NavHostController?) {
        rootNavControllerFlow.update { navController }
    }

    override fun setTabNavController(navController: NavHostController?) {
        tabDestinationListener?.let { listener ->
            tabNavControllerFlow.value?.removeOnDestinationChangedListener(listener)
        }
        tabNavControllerFlow.update { navController }
        if (navController != null) {
            NavController.OnDestinationChangedListener { _, destination, _ ->
                _currentTab.value = destination.getTabNavRoute()
            }.let { listener ->
                tabDestinationListener = listener
                navController.addOnDestinationChangedListener(listener)
            }
            _currentTab.value = navController.currentDestination?.getTabNavRoute()
        } else {
            _currentTab.value = null
        }
    }

    override fun isAtMainScreen(): Boolean {
        val currentBackStackEntry = rootNavControllerFlow.value?.currentBackStackEntry
        val hasTabContainerRoute =
            currentBackStackEntry?.destination?.hasRoute<NavRoute.TabContainer>()
        val route = rootNavControllerFlow.value?.currentBackStackEntry?.destination?.route
        log.d { "Current screen $route" }
        return hasTabContainerRoute ?: false
    }

    override fun isAtHomeTab(): Boolean {
        val currentBackStackEntry = tabNavControllerFlow.value?.currentBackStackEntry
        val hasTabHomeRoute =
            currentBackStackEntry?.destination?.hasRoute<NavRoute.TabHome>() ?: false
        val route = tabNavControllerFlow.value?.currentBackStackEntry?.destination?.route
        log.d { "Current tab $route" }
        return isAtMainScreen() && hasTabHomeRoute
    }


    override fun navigate(
        destination: NavRoute,
        customSetup: (NavOptionsBuilder) -> Unit,
        onCompleted: (() -> Unit)?
    ) {
        scope.launch {
            runCatching {
                getRootNavController()?.navigate(destination) {
                    customSetup(this)
                }
            }.onFailure { e ->
                log.e(e) { "Failed to navigate to $destination" }
            }
            onCompleted?.invoke()
        }
    }

    override fun navigateToTab(
        destination: TabNavRoute,
        saveStateOnPopUp: Boolean,
        shouldLaunchSingleTop: Boolean,
        shouldRestoreState: Boolean
    ) {
        log.d { "Navigating to tab $destination " }
        scope.launch {
            if (!isAtMainScreen()) {
                val rootNav = getRootNavController() ?: return@launch
                val isTabContainerInBackStack = rootNav.currentBackStack.value.any {
                    it.destination.hasRoute(NavRoute.TabContainer::class)
                }
                if (isTabContainerInBackStack) {
                    rootNav.popBackStack(NavRoute.TabContainer, inclusive = false)
                } else {
                    rootNav.navigate(NavRoute.TabContainer) {
                        launchSingleTop = true
                    }
                }
            }
            val tabNav = getTabNavController() ?: return@launch
            tabNav.navigate(destination) {
                popUpTo(NavRoute.HomeScreenGraphKey) {
                    saveState = saveStateOnPopUp
                }
                launchSingleTop = shouldLaunchSingleTop
                restoreState = shouldRestoreState
            }
        }
    }

    override fun navigateBackTo(
        destination: NavRoute,
        shouldInclusive: Boolean,
        shouldSaveState: Boolean
    ) {
        scope.launch {
            getRootNavController()?.popBackStack(
                route = destination,
                inclusive = shouldInclusive,
                saveState = shouldSaveState
            )
        }
    }

    override fun navigateFromUri(uri: String) {
        scope.launch {
            val navUri = NavUri(uri)
            val rootNavController = getRootNavController() ?: return@launch
            if (rootNavController.graph.hasDeepLink(navUri)) {
                val navOptions = navOptions {
                    launchSingleTop = true
                }
                rootNavController.navigate(navUri, navOptions)
            } else if (isAtMainScreen()) {
                val tabNavController = getTabNavController() ?: return@launch
                if (tabNavController.graph.hasDeepLink(navUri)) {
                    val navOptions = navOptions {
                        popUpTo(NavRoute.HomeScreenGraphKey) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    tabNavController.navigate(navUri, navOptions)
                } else {
                    // ignore
                }
            }
        }
    }

    override fun navigateBack(onCompleted: (() -> Unit)?) {
        scope.launch {
            getRootNavController()?.let { rootNavController ->
                if (rootNavController.currentBackStack.value.size > 1) {
                    rootNavController.popBackStack()
                }
            }
            onCompleted?.invoke()
        }
    }

    override fun showBackButton() =
        rootNavControllerFlow.value?.previousBackStackEntry != null && !isAtMainScreen()

    private fun NavDestination.getTabNavRoute(): TabNavRoute? {
        return when {
            this.hasRoute<NavRoute.TabHome>() -> NavRoute.TabHome
            this.hasRoute<NavRoute.TabOpenTradeList>() -> NavRoute.TabOpenTradeList
            this.hasRoute<NavRoute.TabOfferbookMarket>() -> NavRoute.TabOfferbookMarket
            this.hasRoute<NavRoute.TabMiscItems>() -> NavRoute.TabMiscItems
            else -> null
        }
    }
}