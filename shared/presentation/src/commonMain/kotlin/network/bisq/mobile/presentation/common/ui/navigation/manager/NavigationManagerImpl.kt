package network.bisq.mobile.presentation.common.ui.navigation.manager

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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.TabNavRoute

class NavigationManagerImpl(
    val coroutineJobsManager: CoroutineJobsManager,
) : NavigationManager,
    Logging {
    companion object {
        private const val GET_CONTROLLER_TIMEOUT_MS = 10000L // 10 seconds timeout for waiting for nav controller
    }

    private val rootNavControllerFlow = MutableStateFlow<NavHostController?>(null)
    private val tabNavControllerFlow = MutableStateFlow<NavHostController?>(null)

    private val _currentTab = MutableStateFlow<TabNavRoute?>(null)
    override val currentTab: StateFlow<TabNavRoute?> = _currentTab.asStateFlow()
    private var tabDestinationListener: NavController.OnDestinationChangedListener? = null

    // Single mutex to serialize all calls that touch NavController.
    private val navMutex = Mutex()

    // External scope, but we always dispatch to Main when touching NavController.
    private val scope get() = coroutineJobsManager.getScope()

    // Suspend until the root controller is available (with timeout).
    private suspend fun getRootNavController(): NavHostController? =
        withTimeoutOrNull(GET_CONTROLLER_TIMEOUT_MS) {
            rootNavControllerFlow.filterNotNull().first()
        } ?: run {
            log.e { "Timed out waiting for root nav controller after ${GET_CONTROLLER_TIMEOUT_MS}ms" }
            null
        }

    // Suspend until the tab controller is available (with timeout).
    private suspend fun getTabNavController(): NavHostController? =
        withTimeoutOrNull(GET_CONTROLLER_TIMEOUT_MS) {
            tabNavControllerFlow.filterNotNull().first()
        } ?: run {
            log.e { "Timed out waiting for tab nav controller after ${GET_CONTROLLER_TIMEOUT_MS}ms" }
            null
        }

    override fun setRootNavController(navController: NavHostController?) {
        rootNavControllerFlow.value = navController
    }

    override fun setTabNavController(navController: NavHostController?) {
        tabDestinationListener?.let { listener ->
            runCatching {
                tabNavControllerFlow.value?.removeOnDestinationChangedListener(listener)
            }.onFailure { e ->
                log.e(e) { "Failed to remove previous tab destination listener" }
            }
        }

        tabNavControllerFlow.value = navController

        if (navController != null) {
            runCatching {
                NavController
                    .OnDestinationChangedListener { _, destination, _ ->
                        _currentTab.value = destination.getTabNavRoute()
                    }.let { listener ->
                        tabDestinationListener = listener
                        navController.addOnDestinationChangedListener(listener)
                    }
                _currentTab.value = navController.currentDestination?.getTabNavRoute()
            }.onFailure { e ->
                log.e(e) { "Failed to initialize tab nav controller (graph may not be ready yet)" }
            }
        } else {
            _currentTab.value = null
        }
    }

    override fun isAtMainScreen(): Boolean {
        val navController = rootNavControllerFlow.value ?: return false
        return runCatching {
            val currentBackStackEntry = navController.currentBackStackEntry
            val hasTabContainerRoute =
                currentBackStackEntry?.destination?.hasRoute<NavRoute.TabContainer>()
            val route = currentBackStackEntry?.destination?.route
            log.d { "Current screen $route" }
            hasTabContainerRoute ?: false
        }.onFailure { e ->
            log.e(e) { "Failed to determine if at main screen (nav graph may not be ready yet)" }
        }.getOrNull() ?: false
    }

    override fun isAtHomeTab(): Boolean {
        val navController = tabNavControllerFlow.value ?: return false
        val isHomeTab =
            runCatching {
                val currentBackStackEntry = navController.currentBackStackEntry
                val hasTabHomeRoute =
                    currentBackStackEntry?.destination?.hasRoute<NavRoute.TabHome>() ?: false
                val route = currentBackStackEntry?.destination?.route
                log.d { "Current tab $route" }
                hasTabHomeRoute
            }.onFailure { e ->
                log.e(e) { "Failed to determine if at home tab (nav graph may not be ready yet)" }
            }.getOrNull() ?: false
        return isAtMainScreen() && isHomeTab
    }

    override fun navigate(
        destination: NavRoute,
        customSetup: (NavOptionsBuilder) -> Unit,
        onCompleted: (() -> Unit)?,
    ) {
        scope.launch {
            val rootNav = getRootNavController()
            if (rootNav != null) {
                navMutex.withLock {
                    runCatching {
                        rootNav.navigate(destination) {
                            customSetup(this)
                        }
                    }.onFailure { e ->
                        log.e(e) { "Failed to navigate to $destination" }
                    }
                }
            }
            onCompleted?.invoke()
        }
    }

    override fun navigateToTab(
        destination: TabNavRoute,
        saveStateOnPopUp: Boolean,
        shouldLaunchSingleTop: Boolean,
        shouldRestoreState: Boolean,
    ) {
        scope.launch {
            val rootNav = getRootNavController() ?: return@launch
            val tabNav = getTabNavController() ?: return@launch
            navMutex.withLock {
                runCatching {
                    if (!isAtMainScreen()) {
                        val isTabContainerInBackStack =
                            rootNav.currentBackStack.value.any {
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
                }.onFailure { e ->
                    log.e(e) { "Failed to prepare tab container navigation" }
                }

                runCatching {
                    tabNav.navigate(destination) {
                        popUpTo(NavRoute.HomeScreenGraphKey) {
                            saveState = saveStateOnPopUp
                        }
                        launchSingleTop = shouldLaunchSingleTop
                        restoreState = shouldRestoreState
                    }
                }.onFailure { e ->
                    log.e(e) { "Failed to navigate to tab $destination" }
                }
            }
        }
    }

    override fun navigateBackTo(
        destination: NavRoute,
        shouldInclusive: Boolean,
        shouldSaveState: Boolean,
    ) {
        scope.launch {
            val rootNav = getRootNavController() ?: return@launch
            navMutex.withLock {
                runCatching {
                    rootNav.popBackStack(
                        route = destination,
                        inclusive = shouldInclusive,
                        saveState = shouldSaveState,
                    )
                }.onFailure { e ->
                    log.e(e) { "Failed to popBackStack to $destination" }
                }
            }
        }
    }

    override fun navigateFromUri(uri: String) {
        scope.launch {
            val rootNavController = getRootNavController() ?: return@launch
            val navUri = NavUri(uri)
            navMutex.withLock {
                if (rootNavController.graph.hasDeepLink(navUri)) {
                    runCatching {
                        val navOptions =
                            navOptions {
                                launchSingleTop = true
                            }
                        rootNavController.navigate(navUri, navOptions)
                    }.onFailure { e ->
                        log.e(e) { "Failed to navigate from uri $uri via root graph" }
                    }
                } else if (isAtMainScreen()) {
                    // Tab controller acquisition moved outside lock
                }
            }
            // Only acquire tab controller if root didn't handle the deep link and we're at main screen
            if (!rootNavController.graph.hasDeepLink(navUri) && isAtMainScreen()) {
                val tabNavController = getTabNavController() ?: return@launch
                navMutex.withLock {
                    if (tabNavController.graph.hasDeepLink(navUri)) {
                        runCatching {
                            val navOptions =
                                navOptions {
                                    popUpTo(NavRoute.HomeScreenGraphKey) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            tabNavController.navigate(navUri, navOptions)
                        }.onFailure { e ->
                            log.e(e) { "Failed to navigate from uri $uri via tab graph" }
                        }
                    } else {
                        // Deep link not handled by tab graph; ignore.
                    }
                }
            }
        }
    }

    override fun navigateBack(onCompleted: (() -> Unit)?) {
        scope.launch {
            val rootNav = getRootNavController()
            if (rootNav != null) {
                navMutex.withLock {
                    runCatching {
                        if (rootNav.currentBackStack.value.size > 1) {
                            rootNav.popBackStack()
                        }
                    }.onFailure { e ->
                        log.e(e) { "Failed to navigate back" }
                    }
                }
            }
            onCompleted?.invoke()
        }
    }

    override fun showBackButton() =
        runCatching {
            val rootNav = rootNavControllerFlow.value
            rootNav?.previousBackStackEntry != null && !isAtMainScreen()
        }.onFailure { e ->
            log.e(e) { "Failed to determine showBackButton state" }
        }.getOrNull() ?: false

    private fun NavDestination.getTabNavRoute(): TabNavRoute? =
        when {
            this.hasRoute<NavRoute.TabHome>() -> NavRoute.TabHome
            this.hasRoute<NavRoute.TabOpenTradeList>() -> NavRoute.TabOpenTradeList
            this.hasRoute<NavRoute.TabOfferbookMarket>() -> NavRoute.TabOfferbookMarket
            this.hasRoute<NavRoute.TabMiscItems>() -> NavRoute.TabMiscItems
            else -> null
        }
}
