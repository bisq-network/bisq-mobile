package network.bisq.mobile.presentation.ui.components.molecules

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class TopBarPresenter(
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val connectivityService: ConnectivityService,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter), ITopBarPresenter {

    override val userProfile: StateFlow<UserProfileVO?> get() = userProfileServiceFacade.selectedUserProfile
    override val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage get() = userProfileServiceFacade::getUserProfileIcon

    override val showAnimation: StateFlow<Boolean> get() = settingsServiceFacade.useAnimations

    override val connectivityStatus: StateFlow<ConnectivityService.ConnectivityStatus> get() = connectivityService.status

    override fun avatarEnabled(currentTab: String?): Boolean {
        return isAtMainScreen() && currentTab != Routes.TabMiscItems.name
    }

    override fun navigateToUserProfile() {
        navigateTo(Routes.UserProfile)
    }
}