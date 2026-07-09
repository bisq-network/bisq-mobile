package network.bisq.mobile.presentation.common.ui.components.molecules

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.service.network.ConnectivityService
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.presentation.common.ui.animation.AnimationSettings
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.TabNavRoute
import network.bisq.mobile.presentation.main.MainPresenter

open class TopBarPresenter(
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    protected val connectivityService: ConnectivityService,
    private val animationSettings: AnimationSettings,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter),
    ITopBarPresenter {
    override val userProfile: StateFlow<UserProfileVO?> get() = userProfileServiceFacade.selectedUserProfile
    override val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage get() = userProfileServiceFacade::getUserProfileIcon

    // Effective flag (user setting AND device not low-spec) so the avatar honours the device lock. #1293
    override val showAnimation: StateFlow<Boolean> get() = animationSettings.enabled

    override val connectivityStatus: StateFlow<ConnectivityService.ConnectivityStatus>
        get() = connectivityService.status

    override fun avatarEnabled(currentTab: TabNavRoute?): Boolean {
        val hasTabMiscItemsRoute = currentTab == NavRoute.TabMiscItems
        return isAtMainScreen() && !hasTabMiscItemsRoute
    }

    override fun navigateToUserProfile() {
        navigateTo(NavRoute.UserProfile)
    }
}
