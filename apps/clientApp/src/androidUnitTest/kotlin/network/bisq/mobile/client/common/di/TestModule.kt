package network.bisq.mobile.client.common.di

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.TabNavRoute
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
val clientTestModule =
    module {
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
                override val currentTab = MutableStateFlow<TabNavRoute?>(null)

                override fun setRootNavController(navController: NavHostController?) {}

                override fun setTabNavController(navController: NavHostController?) {}

                override fun isAtMainScreen(): Boolean = false

                override fun isAtHomeTab(): Boolean = false

                override fun showBackButton(): Boolean = false

                override fun navigate(
                    destination: NavRoute,
                    customSetup: (NavOptionsBuilder) -> Unit,
                    onCompleted: (() -> Unit)?,
                ) {
                }

                override fun navigateToTab(
                    destination: TabNavRoute,
                    saveStateOnPopUp: Boolean,
                    shouldLaunchSingleTop: Boolean,
                    shouldRestoreState: Boolean,
                ) {
                }

                override fun navigateBackTo(
                    destination: NavRoute,
                    shouldInclusive: Boolean,
                    shouldSaveState: Boolean,
                ) {
                }

                override fun navigateFromUri(uri: String) {}

                override fun navigateBack(onCompleted: (() -> Unit)?) {}
            }
        }

        // Provide a mock ITopBarPresenter
        single<ITopBarPresenter> {
            object : ITopBarPresenter {
                override val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage = { _ ->
                    PlatformImage(ImageBitmap(1, 1))
                }
                override val userProfile: StateFlow<UserProfileVO?> = MutableStateFlow(null)
                override val showAnimation: StateFlow<Boolean> = MutableStateFlow(false)
                override val connectivityStatus: StateFlow<ConnectivityService.ConnectivityStatus> =
                    MutableStateFlow(ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED)
                override val isInteractive: StateFlow<Boolean> = MutableStateFlow(true)

                override fun avatarEnabled(currentTab: TabNavRoute?): Boolean = false

                override fun navigateToUserProfile() {}

                override fun onViewAttached() {}

                override fun onViewUnattaching() {}

                override fun onDestroying() {}

                override fun onMainBackNavigation() {}

                override fun isDemo(): Boolean = false

                override fun isSmallScreen(): Boolean = false

                override fun onCloseGenericErrorPanel() {}

                override fun navigateToReportError() {}

                override fun isIOS(): Boolean = false

                override fun getSnackState(): SnackbarHostState = SnackbarHostState()

                override fun showSnackbar(
                    message: String,
                    isError: Boolean,
                    duration: SnackbarDuration,
                ) {
                }

                override fun dismissSnackbar() {}

                override fun isAtHomeTab(): Boolean = false

                override fun navigateToTab(
                    destination: TabNavRoute,
                    saveStateOnPopUp: Boolean,
                    shouldLaunchSingleTop: Boolean,
                    shouldRestoreState: Boolean,
                ) {
                }
            }
        }
    }
