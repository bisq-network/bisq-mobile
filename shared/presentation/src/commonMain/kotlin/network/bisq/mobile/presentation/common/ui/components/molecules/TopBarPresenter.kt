package network.bisq.mobile.presentation.common.ui.components.molecules

import kotlinx.coroutines.flow.StateFlow

import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.TabNavRoute

open class TopBarPresenter(
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    protected val connectivityService: ConnectivityService,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter), ITopBarPresenter {

    override val userProfile: StateFlow<UserProfileVO?> get() = userProfileServiceFacade.selectedUserProfile
    override val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage get() = userProfileServiceFacade::getUserProfileIcon

    override val showAnimation: StateFlow<Boolean> get() = settingsServiceFacade.useAnimations

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