package network.bisq.mobile.presentation.ui.navigation.manager

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavUri
import androidx.navigation.navOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.ui.navigation.NavRoute
import network.bisq.mobile.presentation.ui.navigation.TabNavRoute

class NavigationManagerImpl(
    val coroutineJobsManager: CoroutineJobsManager,
) : NavigationManager, Logging {

    private var rootNavControllerFlow = MutableStateFlow<NavHostController?>(null)
    private var tabNavControllerFlow = MutableStateFlow<NavHostController?>(null)

    private val _currentTab = MutableStateFlow<TabNavRoute?>(null)
    override val currentTab: StateFlow<TabNavRoute?> = _currentTab.asStateFlow()
    private var tabDestinationListener: NavController.OnDestinationChangedListener? = null

    // Single mutex to serialize all calls that touch NavController.
    private val navMutex = Mutex()

    // External scope, but we always dispatch to Main when touching NavController.
    private val scope get() = coroutineJobsManager.getScope()

    // Suspend until the root controller is available *and* its navigation graph is ready,
    // always collecting and checking on the Main dispatcher.
    private suspend fun getRootNavController(): NavHostController {
        val controller = rootNavControllerFlow.mapNotNull { it }.first()
        controller.awaitGraphReady()
        return controller
    }

    // Suspend until the tab controller is available *and* its navigation graph is ready,
    // always collecting and checking on the Main dispatcher.
    private suspend fun getTabNavController(): NavHostController {
        val controller = tabNavControllerFlow.mapNotNull { it }.first()
        controller.awaitGraphReady()
        return controller
    }

    override fun setRootNavController(navController: NavHostController?) {
        // Ensure we set on Main to avoid thread checks in NavController internals.
        scope.launch {
            rootNavControllerFlow.value = navController
        }
    }

    override fun setTabNavController(navController: NavHostController?) {
        // Ensure listener operations happen on Main and are protected.
        scope.launch {
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
                    NavController.OnDestinationChangedListener { _, destination, _ ->
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
        val isHomeTab = runCatching {
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
            navMutex.withLock {
                try {
                    val rootNav = getRootNavController()
                    runCatching {
                        rootNav.navigate(destination) {
                            customSetup(this)
                        }
                    }.onFailure { e ->
                        log.e(e) { "Failed to navigate to $destination" }
                    }
                } catch (t: Throwable) {
                    log.e(t) { "Failed to navigate to $destination (exception)" }
                } finally {
                    onCompleted?.invoke()
                }
            }
        }
    }

    override fun navigateToTab(
        destination: TabNavRoute,
        saveStateOnPopUp: Boolean,
        shouldLaunchSingleTop: Boolean,
        shouldRestoreState: Boolean,
    ) {
        scope.launch {
            navMutex.withLock {
                try {
                    val rootNav = getRootNavController()
                    runCatching {
                        if (!isAtMainScreen()) {
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
                    }.onFailure { e ->
                        log.e(e) { "Failed to prepare tab container navigation" }
                    }

                    val tabNav = getTabNavController()
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
                } catch (t: Throwable) {
                    log.e(t) { "Failed to navigate to tab $destination (exception)" }
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
            navMutex.withLock {
                try {
                    val rootNav = getRootNavController()
                    runCatching {
                        rootNav.popBackStack(
                            route = destination,
                            inclusive = shouldInclusive,
                            saveState = shouldSaveState,
                        )
                    }.onFailure { e ->
                        log.e(e) { "Failed to popBackStack to $destination" }
                    }
                } catch (t: Throwable) {
                    log.e(t) { "Failed to navigate back to $destination (exception)" }
                }
            }
        }
    }

    override fun navigateFromUri(uri: String) {
        scope.launch {
            navMutex.withLock {
                try {
                    val navUri = NavUri(uri)
                    val rootNavController = getRootNavController()
                    if (rootNavController.graph.hasDeepLink(navUri)) {
                        runCatching {
                            val navOptions = navOptions {
                                launchSingleTop = true
                            }
                            rootNavController.navigate(navUri, navOptions)
                        }.onFailure { e ->
                            log.e(e) { "Failed to navigate from uri $uri via root graph" }
                        }
                    } else if (isAtMainScreen()) {
                        val tabNavController = getTabNavController()
                        if (tabNavController.graph.hasDeepLink(navUri)) {
                            runCatching {
                                val navOptions = navOptions {
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
                } catch (t: Throwable) {
                    log.e(t) { "Failed to navigate from uri $uri (exception)" }
                }
            }
        }
    }

    override fun navigateBack(onCompleted: (() -> Unit)?) {
        scope.launch {
            navMutex.withLock {
                try {
                    val rootNav = getRootNavController()
                    runCatching {
                        if (rootNav.currentBackStack.value.size > 1) {
                            rootNav.popBackStack()
                        }
                    }.onFailure { e ->
                        log.e(e) { "Failed to navigate back" }
                    }
                } catch (t: Throwable) {
                    log.e(t) { "Failed to navigate back (exception)" }
                } finally {
                    onCompleted?.invoke()
                }
            }
        }
    }

    override fun showBackButton() =
        runCatching {
            val rootNav = rootNavControllerFlow.value
            rootNav?.previousBackStackEntry != null && !isAtMainScreen()
        }.onFailure { e ->
            log.e(e) { "Failed to determine showBackButton state" }
        }.getOrNull() ?: false

    /**
     * Await until the NavController has an attached navigation graph.
     *
     * When the NavHost is not yet composed, accessing [graph] or related APIs can throw
     * `IllegalStateException("You must call setGraph() before calling getGraph()")`.
     * We treat *only* that specific situation as transient and retry for a short, bounded
     * period. Any other exception is rethrown so real issues are not hidden.
     */
    private suspend fun NavHostController.awaitGraphReady(
        maxAttempts: Int = 60,
        delayMs: Long = 16L,
    ) {
        repeat(maxAttempts) { attemptIndex ->
            val isReady = try {
                // Touching `graph` is enough to trigger the internal readiness checks.
                @Suppress("UNUSED_VARIABLE")
                val ignored = this.graph
                true
            } catch (e: IllegalStateException) {
                val message = e.message ?: ""
                if (message.contains("setGraph() before calling getGraph()")) {
                    false
                } else {
                    // Different IllegalStateException – propagate it, as it's not the
                    // expected transient "graph not ready" condition.
                    throw e
                }
            }

            if (isReady) return

            if (attemptIndex == maxAttempts - 1) {
                log.w {
                    "NavController graph not ready after ${maxAttempts * delayMs}ms; continuing without waiting further"
                }
                return
            }

            // Small delay to yield and give Compose/NavHost time to attach the graph.
            delay(delayMs)
        }
    }

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

