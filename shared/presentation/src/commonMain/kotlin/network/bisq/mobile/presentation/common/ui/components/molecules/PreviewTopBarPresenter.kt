package network.bisq.mobile.presentation.common.ui.components.molecules

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.service.network.ConnectivityService
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.presentation.common.ui.base.SnackbarPosition
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.TabNavRoute

/**
 * Lightweight presenter used ONLY for compose @Preview. Provides the minimum surface
 * so TopBar and scaffolds can render without real DI/navigation.
 */
class PreviewTopBarPresenter : ITopBarPresenter {
    override val showAnimation: StateFlow<Boolean> = MutableStateFlow(false)

    private val _userProfile: MutableStateFlow<UserProfileVO?> = MutableStateFlow(null)
    override val userProfile: StateFlow<UserProfileVO?> = _userProfile.asStateFlow()
    override val userProfileIcon: StateFlow<PlatformImage?> = MutableStateFlow(null)

    override val connectivityStatus: StateFlow<ConnectivityService.ConnectivityStatus> =
        MutableStateFlow(ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED)

    private val snackbar = SnackbarHostState()

    override fun avatarEnabled(currentTab: TabNavRoute?): Boolean = false

    override fun navigateToUserProfile() {}

    override fun isDemo(): Boolean = false

    override fun isSmallScreen(): Boolean = false

    override fun onCloseGenericErrorPanel() {}

    override fun navigateToReportError() {}

    override fun isIOS(): Boolean = false

    override fun showSnackbar(
        message: String,
        type: SnackbarType,
        position: SnackbarPosition,
        duration: SnackbarDuration,
    ) {
    }

    override fun isAtHomeTab(): Boolean = true

    override fun navigateToTab(
        destination: TabNavRoute,
        saveStateOnPopUp: Boolean,
        shouldLaunchSingleTop: Boolean,
        shouldRestoreState: Boolean,
    ) {}

    override fun onMainBackNavigation() {}

    override fun onViewAttached() {}

    override fun onViewUnattaching() {}

    override fun onDestroying() {}
}
