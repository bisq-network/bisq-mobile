package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.NavRoute

class ClientSplashPresenter(
    mainPresenter: MainPresenter,
    userProfileService: UserProfileServiceFacade,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    settingsRepository: SettingsRepository,
    settingsServiceFacade: SettingsServiceFacade,
    private val webSocketClientProvider: WebSocketClientProvider,
) : SplashPresenter(
    mainPresenter,
    applicationBootstrapFacade,
    userProfileService,
    settingsRepository,
    settingsServiceFacade,
) {
    override val state: StateFlow<String> get() = applicationBootstrapFacade.state

    override fun doCustomNavigationLogic(settings: Settings, hasProfile: Boolean): Boolean {
        when {
            settings.bisqApiUrl.isEmpty() -> navigateToTrustedNodeSetup()
            settings.bisqApiUrl.isNotEmpty() && !hasProfile -> navigateToCreateProfile()
            else -> navigateToHome()
        }
        return true
    }

    override suspend fun navigateToNextScreen() {
        if (!webSocketClientProvider.isConnected()) {
            log.d { "No connectivity detected, navigating to trusted node setup" }
            navigateToTrustedNodeSetup()
            return
        }
        super.navigateToNextScreen()
    }

    private fun navigateToTrustedNodeSetup() {
        navigateTo(NavRoute.TrustedNodeSetup) {
            it.popUpTo(NavRoute.Splash) { inclusive = true }
        }
    }
}