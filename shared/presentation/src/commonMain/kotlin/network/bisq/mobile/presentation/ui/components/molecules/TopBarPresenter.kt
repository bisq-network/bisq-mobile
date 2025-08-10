package network.bisq.mobile.presentation.ui.components.molecules

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class TopBarPresenter(
    private val userRepository: UserRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val connectivityService: ConnectivityService,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), ITopBarPresenter {

    private val _uniqueAvatar = MutableStateFlow(userRepository.data.value?.uniqueAvatar)
    override val uniqueAvatar: StateFlow<PlatformImage?> get() = _uniqueAvatar.asStateFlow()

    private fun setUniqueAvatar(value: PlatformImage?) {
        _uniqueAvatar.value = value
    }

    override val showAnimation: StateFlow<Boolean> get() = settingsServiceFacade.useAnimations

    override val connectivityStatus: StateFlow<ConnectivityService.ConnectivityStatus> get() = connectivityService.status

    override fun onViewAttached() {
        super.onViewAttached()
        refresh()
    }

    private fun refresh() {
        launchUI {
            val uniqueAvatar = withContext(IODispatcher) {
                userRepository.fetch()?.uniqueAvatar
            }
            setUniqueAvatar(uniqueAvatar)
        }
    }

    override fun avatarEnabled(currentTab: String?): Boolean {
        return isAtMainScreen() && currentTab != Routes.TabSettings.name
    }

    override fun navigateToUserProfile() {
        navigateTo(Routes.UserProfileSettings)
    }
}