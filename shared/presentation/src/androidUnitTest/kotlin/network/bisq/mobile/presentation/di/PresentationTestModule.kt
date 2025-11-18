package network.bisq.mobile.presentation.di

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.ui.GlobalUiManager
import network.bisq.mobile.presentation.ui.navigation.NavRoute
import network.bisq.mobile.presentation.ui.navigation.TabNavRoute
import network.bisq.mobile.presentation.ui.navigation.manager.NavigationManager
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
val presentationTestModule = module {
    // Exception handler setup - singleton to ensure consistent setup
    single<CoroutineExceptionHandlerSetup> { CoroutineExceptionHandlerSetup() }

    // Job managers - factory to ensure each component has its own instance
    factory<CoroutineJobsManager> {
        DefaultCoroutineJobsManager().apply {
            get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
        }
    }

    // Provide a test dispatcher-based GlobalUiManager
    single { GlobalUiManager(UnconfinedTestDispatcher()) }

    // Provide a default NavigationManager stub
    single<NavigationManager> {
        object : NavigationManager {
            override val currentTab = kotlinx.coroutines.flow.MutableStateFlow<TabNavRoute?>(null)
            override fun setRootNavController(navController: androidx.navigation.NavHostController?) {}
            override fun setTabNavController(navController: androidx.navigation.NavHostController?) {}
            override fun isAtMainScreen(): Boolean = false
            override fun isAtHomeTab(): Boolean = false
            override fun showBackButton(): Boolean = false
            override fun navigate(destination: NavRoute, customSetup: (androidx.navigation.NavOptionsBuilder) -> Unit, onCompleted: (() -> Unit)?) {}
            override fun navigateToTab(destination: TabNavRoute, saveStateOnPopUp: Boolean, shouldLaunchSingleTop: Boolean, shouldRestoreState: Boolean) {}
            override fun navigateBackTo(destination: NavRoute, shouldInclusive: Boolean, shouldSaveState: Boolean) {}
            override fun navigateFromUri(uri: String) {}
            override fun navigateBack(onCompleted: (() -> Unit)?) {}
        }
    }
}

