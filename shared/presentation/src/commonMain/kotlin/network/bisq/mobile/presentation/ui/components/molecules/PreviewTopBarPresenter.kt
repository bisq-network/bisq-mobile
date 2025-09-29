package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.presentation.ui.navigation.Routes

/**
 * Lightweight presenter used ONLY for compose @Preview. Provides the minimum surface
 * so TopBar and scaffolds can render without real DI/navigation.
 */
class PreviewTopBarPresenter(
    private val rootNav: NavHostController,
    private val tabNav: NavHostController,
) : ITopBarPresenter {

    private val _isInteractive = MutableStateFlow(true)
    override val isInteractive: StateFlow<Boolean> get() = _isInteractive

    override val showAnimation: StateFlow<Boolean> = MutableStateFlow(false)

    private val _userProfile: MutableStateFlow<UserProfileVO?> = MutableStateFlow(null)
    override val userProfile: StateFlow<UserProfileVO?> get() = _userProfile.asStateFlow()
    override val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage get() = this::getUserProfileIcon

    override val connectivityStatus: StateFlow<ConnectivityService.ConnectivityStatus> =
        MutableStateFlow(ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED)

    private val snackbar = SnackbarHostState()

    override fun avatarEnabled(currentTab: String?): Boolean = false
    override fun navigateToUserProfile() {}

    override fun isDemo(): Boolean = false
    override fun isSmallScreen(): Boolean = false
    override fun onCloseGenericErrorPanel() {}
    override fun navigateToReportError() {}
    override fun isIOS(): Boolean = false

    override fun getRootNavController(): NavHostController = rootNav
    override fun getRootTabNavController(): NavHostController = tabNav

    override fun getSnackState(): SnackbarHostState = snackbar
    override fun showSnackbar(message: String, isError: Boolean, duration: SnackbarDuration) {}
    override fun dismissSnackbar() {}
    override fun isAtHome(): Boolean = true
    override fun navigateToTab(
        destination: Routes,
        saveStateOnPopUp: Boolean,
        shouldLaunchSingleTop: Boolean,
        shouldRestoreState: Boolean
    ) {
    }

    override fun goBack(): Boolean = false
    override fun onMainBackNavigation() {}
    override fun onViewAttached() {}
    override fun onViewUnattaching() {}
    override fun onDestroying() {}

    fun getUserProfileIcon(userProfile: UserProfileVO): PlatformImage {
        // As _userProfile is null the composable triggering that method will never be created.
        // We show the dummy_user_profile_icon.png instead in the composable.
        throw UnsupportedOperationException("getUserProfileIcon method in PreviewTopBarPresenter is not supported")
    }
}

