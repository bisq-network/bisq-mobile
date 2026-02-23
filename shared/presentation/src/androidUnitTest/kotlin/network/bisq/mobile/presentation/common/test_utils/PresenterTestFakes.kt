package network.bisq.mobile.presentation.common.test_utils

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.TabNavRoute
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager

class TestCoroutineJobsManager(
    dispatcher: CoroutineDispatcher,
    override var coroutineExceptionHandler: ((Throwable) -> Unit)? = null,
) : CoroutineJobsManager {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val jobs = mutableSetOf<Job>()

    override suspend fun dispose() {
        scope.cancel()
        jobs.clear()
    }

    override fun getScope(): CoroutineScope = scope
}

class NoopNavigationManager : NavigationManager {
    private val _currentTab = MutableStateFlow<TabNavRoute?>(null)
    override val currentTab: StateFlow<TabNavRoute?> get() = _currentTab.asStateFlow()

    override fun setRootNavController(navController: NavHostController?) {
    }

    override fun setTabNavController(navController: NavHostController?) {
    }

    override fun isAtMainScreen(): Boolean = true

    override fun isAtHomeTab(): Boolean = true

    override fun showBackButton(): Boolean = false

    override fun navigate(
        destination: NavRoute,
        customSetup: (NavOptionsBuilder) -> Unit,
        onCompleted: (() -> Unit)?,
    ) {
        onCompleted?.invoke()
    }

    override fun navigateToTab(
        destination: TabNavRoute,
        saveStateOnPopUp: Boolean,
        shouldLaunchSingleTop: Boolean,
        shouldRestoreState: Boolean,
    ) {
        _currentTab.value = destination
    }

    override fun navigateBackTo(
        destination: NavRoute,
        shouldInclusive: Boolean,
        shouldSaveState: Boolean,
    ) {
    }

    override fun navigateFromUri(uri: String) {
    }

    override fun navigateBack(onCompleted: (() -> Unit)?) {
        onCompleted?.invoke()
    }
}
