package network.bisq.mobile.presentation.ui.components.molecules

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class TopBarPresenter(
    userRepository: UserRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val connectivityService: ConnectivityService,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), ITopBarPresenter {

    override val uniqueAvatar: StateFlow<PlatformImage?> =
        userRepository.data.map { it.uniqueAvatar }
            .stateIn(presenterScope, SharingStarted.Lazily, null)

    override val showAnimation: StateFlow<Boolean> get() = settingsServiceFacade.useAnimations

    override val connectivityStatus: StateFlow<ConnectivityService.ConnectivityStatus> get() = connectivityService.status

    override fun avatarEnabled(currentTab: String?): Boolean {
        return isAtMainScreen() && currentTab != Routes.TabMiscItems.name
    }

    override fun navigateToUserProfile() {
        navigateTo(Routes.UserProfile)
    }
}